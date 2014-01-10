/*
 * RunningTask.java
 */
package org.ngbw.sdk.database;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ngbw.sdk.Workbench;
import org.ngbw.sdk.WorkbenchException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.lang.Thread;

import org.ngbw.sdk.common.util.ProcessUtils;
import org.ngbw.sdk.tool.Tool;



/**
 * 
 * @author Terri Liebowitz Schwartz 
 *
 */
 /*
 	Schema:
		CREATE TABLE `running_tasks` (
		  `JOBHANDLE` varchar(255) NOT NULL,			- set in ctor
		  `RESOURCE` varchar(100) NOT NULL,				- set in ctor
		  `WORKSPACE` varchar(2000) NOT NULL,			- set in ctor
		  `TOOL_ID` varchar(100) NOT NULL,				- set in ctor
		  `USER_ID` bigint(20) NOT NULL,				- set in ctor	
		  `TASK_ID` bigint(20) NOT NULL,				- set in ctor
		  `SUBMITTER` varchar(1023) NOT NULL,			- set in ctor

		  `REMOTE_JOB_ID` varchar(1023) DEFAULT NULL,	- set by WorkQueue.submitted() 

		  `DATE_ENTERED` datetime NOT NULL,				- set in ctor

		  `LOCKED` datetime DEFAULT NULL,				- these 3 fields are set by RunningTask.lock, RunningTask.unlock 
		  `HOSTNAME` varchar(200),
		  `PID` bigint(20),

		  `STATUS` varchar(100) NOT NULL,				- set by ctor to STATUS_NEW, changed by RunningTask.changeStatus()
		  `ATTEMPTS` int(4) NOT NULL,					- set by WorkQueue.tryAgain() -> RunningTask.incrementAttempts()

		  `OUTPUT_DESC` varchar(5000) DEFAULT NULL,		- set by WorkQueue.updateWork() after pise and params have been processed
		  `COMMANDLINE` varchar(5000) DEFAULT NULL,		- set by WorkQueue.updateWork()
		  `SPROPS` varchar(500) DEFAULT NULL,			- set by WorkQueue.updateWork(), has scheduler properties
		  PRIMARY KEY (`JOBHANDLE`)
		) ;

	1. RunningTask is created TaskInitate.queueTask calls WorkQueue.postWork(task, tool, tgChargeNumber) 
	Ctor sets status = STATUS_NEW.

	2. SubmitJobD updates the RunningTask via TaskInitiate.runTask -> WorkQueue.updateWork(RunningTask, Tool, RenderedCommand) ->
	RunningTask.updateWork(RunningTask, Tool, RenderedCommand) after processing the pise xml and parameters.  This isn't done
	in the initial step because it may be too much work to do on an interactive application thread.

	3. SubmitJobD submits the job and calls WorkQueue.submitted(RunningTask, remoteJobId).

	4. Attempts is incremented by WorkQueue.tryAgain -> RunningTask.incrementAttempts.  tryAgain is called
	in SubmitJobD by calling TaskInitate.runTask and in loadResultsD via TaskMonitor.getResults().

	5. SubmitJobD,  LoadResultsD and RecoverResultsD lock records before processing.  They do so by setting the LOCKED,
	HOSTNAME and PID fields.  On startup the daemons look for locked records that they would process and unlock them
	if the PID in the record is no longer running. (They only do this if the host they are running on has the same HOSTNAME
	as the record).  The LOCKED, HOSTNAME and PID fields are only altered via RunningTask.lock and unlock, not the usual
	way of RunningTask.save().

	6. RunningTask.changeStatus is called from WorkQueue methods: submitted() and markDone().

	7. WorkQueue.fail() and WorkQueue.finish() remove the RunningTask record when job processing is over.
		

 */
public class RunningTask extends Row implements Comparable<RunningTask> {
	private static final Log log = LogFactory.getLog(RunningTask.class.getName());
	public static final String STATUS_NEW = "NEW";
		// received request to run 
	public static final String STATUS_SUBMITTED = "SUBMITTED";
		// job submitted to remote queueing system
	public static final String STATUS_TERMINATED = "TERMINATED";
		// job no longer running on remote host


	// Lock has expired when NOW() > DATE_ADD(LOCKED,  INTERVAL 1 HOUR), i.e. lock was set more than an hour ago.
	//private static final String andIsNotLocked = " AND (LOCKED IS NULL OR DATE_ADD(LOCKED, INTERVAL 2 HOUR) < NOW())"; 
	private static final String andIsNotLocked = " AND (LOCKED IS NULL ) " ;
	private static final String andIsLocked = " AND (LOCKED IS NOT NULL ) " ;

	/**
	 *
	 */
	private class AddParameterOp implements RowOperation {

		private final String m_name;
		private final String m_value;


		// constructors


		protected AddParameterOp(String parameter, String value)
		{
			if (parameter == null)
				throw new NullPointerException("parameter");

			m_name = parameter;
			m_value = value;
		}


		// public methods


		public void execute(Connection dbConn) throws SQLException, IOException
		{
			PreparedStatement insertStmt = dbConn.prepareStatement("INSERT INTO running_tasks_parameters (JOBHANDLE, NAME, VALUE) VALUES (?, ?, ?)");

			try {
				m_jobhandle.setParameter(insertStmt, 1);
				insertStmt.setString(2, m_name);

				if (m_value != null)
					insertStmt.setString(3, m_value);
				else
					insertStmt.setNull(3, Types.VARCHAR);

				insertStmt.executeUpdate();
			}
			finally {
				insertStmt.close();
			}
		}
	}

	/**
	 *
	 */
	private class SetParameterOp implements RowOperation {

		private final String m_name;
		private final String m_value;


		// constructors


		protected SetParameterOp(String parameter, String value)
		{
			if (parameter == null)
				throw new NullPointerException("parameter");

			m_name = parameter;
			m_value = value;
		}


		// public methods


		public void execute(Connection dbConn) throws SQLException, IOException
		{
			PreparedStatement updateStmt = dbConn.prepareStatement("UPDATE running_tasks_parameters SET VALUE = ? WHERE JOBHANDLE= ? AND NAME = ?");

			try {
				if (m_value != null)
					updateStmt.setString(1, m_value);
				else
					updateStmt.setNull(1, Types.VARCHAR);

				m_jobhandle.setParameter(updateStmt, 2);
				updateStmt.setString(3, m_name);

				updateStmt.executeUpdate();
			}
			finally {
				updateStmt.close();
			}
		}
	}

	/**
	 *
	 */
	private class RemoveParameterOp implements RowOperation {

		private final String m_name;


		// constructors


		protected RemoveParameterOp(String parameter)
		{
			if (parameter == null)
				throw new NullPointerException("parameter");

			m_name = parameter;
		}


		// public methods


		public void execute(Connection dbConn) throws SQLException, IOException
		{
			PreparedStatement deleteStmt = dbConn.prepareStatement("DELETE FROM running_tasks_parameters WHERE JOBHANDLE= ? AND NAME = ?");

			try {
				m_jobhandle.setParameter(deleteStmt, 1);
				deleteStmt.setString(2, m_name);

				deleteStmt.executeUpdate();
			}
			finally {
				deleteStmt.close();
			}
		}
	}

	/**
	 *
	 */
	private class RemoveAllParametersOp implements RowOperation {

		// public methods


		public void execute(Connection dbConn) throws SQLException
		{
			deleteParameters(dbConn, m_jobhandle.getValue());
		}
	}

	/**
	 *
	 */
	private class ParameterMap extends MonitoredMap<String, String> {

		// constructors


		protected ParameterMap(Map<String, String> prefMap)
		{
			super(prefMap);
		}


		// protected methods


		protected void addMapPutOp(String key, String value)
		{
			m_opQueue.add(new AddParameterOp(key, value));
		}

		protected void addMapSetOp(String key, String oldValue, String newValue)
		{
			m_opQueue.add(new SetParameterOp(key, newValue));
		}

		protected void addMapRemoveOp(String key)
		{
			m_opQueue.add(new RemoveParameterOp(key));
		}

		protected void addMapClearOp()
		{
			m_opQueue.add(new RemoveAllParametersOp());
		}
	}


	private static final String TABLE_NAME = "running_tasks";
	private static final String COLUMN_NAMES = 
			TABLE_NAME + ".JOBHANDLE, " + 
			TABLE_NAME + ".RESOURCE, " + 
			TABLE_NAME + ".TOOL_ID, " + 
			TABLE_NAME + ".USER_ID, " + 
			TABLE_NAME + ".TASK_ID, " +
			TABLE_NAME + ".SUBMITTER, " + 
			TABLE_NAME + ".REMOTE_JOB_ID, " + 
			TABLE_NAME + ".DATE_ENTERED, " + 
			TABLE_NAME + ".LOCKED, " + 

			TABLE_NAME + ".HOSTNAME, " +
			TABLE_NAME + ".PID, " +

			TABLE_NAME + ".STATUS, " +
			TABLE_NAME + ".ATTEMPTS, " +
			TABLE_NAME + ".OUTPUT_DESC, " +
			TABLE_NAME + ".COMMANDLINE, " + 
			TABLE_NAME + ".SPROPS "; 

	// not-null = false, nullable = true
	private StringColumn m_resource = new StringColumn(TABLE_NAME + ".RESOURCE", false, 100);
	private StringColumn m_tool_id = new StringColumn(TABLE_NAME + ".TOOL_ID", false, 100);
	private LongColumn m_user_id = new LongColumn(TABLE_NAME + ".USER_ID", false);
	private LongColumn m_task_id = new LongColumn(TABLE_NAME + ".TASK_ID", false);
	private StringColumn m_submitter = new StringColumn(TABLE_NAME + ".SUBMITTER", false, 1023);
	private StringColumn m_remote_job_id = new StringColumn(TABLE_NAME + ".REMOTE_JOB_ID", true, 1023);
	private DateColumn m_date_entered = new DateColumn(TABLE_NAME + ".DATE_ENTERED", false);
	private DateColumn m_locked = new DateColumn(TABLE_NAME + ".LOCKED", true);

	private StringColumn m_hostname = new StringColumn(TABLE_NAME + ".HOSTNAME", true, 200);
	private LongColumn m_pid = new LongColumn(TABLE_NAME + ".PID", true);

	private StringColumn m_status = new StringColumn(TABLE_NAME + ".STATUS", false, 255);
	private IntegerColumn  m_attempts = new IntegerColumn(TABLE_NAME + ".ATTEMPTS", false);
	private StringColumn m_jobhandle = new StringColumn(TABLE_NAME + ".JOBHANDLE", false, 255);
	private StringColumn m_output_desc = new StringColumn(TABLE_NAME + ".OUTPUT_DESC", true, 5000);
	private StringColumn m_commandline = new StringColumn(TABLE_NAME + ".COMMANDLINE", true, 5000);
	private StringColumn m_sprops = new StringColumn(TABLE_NAME + ".SPROPS", true, 500);
	private List<RowOperation> m_opQueue = new ArrayList<RowOperation>();
	private ParameterMap m_parameters;

	private boolean m_isNew = true;


	 // Find an existing record in the database.
	 public static RunningTask find(String jobhandle) throws IOException, SQLException
    {
        RunningTask rt = new RunningTask();
        rt.setJobhandle(jobhandle);
        try
        {
            rt.refresh();
        }
        catch (NotExistException nee)
        {
            return null;
        }
		rt.getTask();
        return rt;
    }


	// constructors
    private RunningTask()
    {
        super(TABLE_NAME);
        m_isNew = false;
    }


	/* Creates new record that doesn't exist in db until this is saved */
	public RunningTask(Task task, String resource, String submitter, String chargeNumber) throws SQLException, IOException
	{
		super(TABLE_NAME);
		m_jobhandle.setValue(task.getJobHandle());
		m_resource.setValue(resource);

		m_submitter.setValue(submitter);
		m_task_id.setValue(task.getTaskId());
		m_tool_id.setValue(task.getToolId());
		m_user_id.setValue(task.getUserId());

		m_date_entered.setValue(Calendar.getInstance().getTime());
		m_status.setValue(STATUS_NEW);
		m_attempts.setValue(0);

		m_task = task;
		triedToFindTask = true;
		if (chargeNumber != null)
		{
            this.parameters().put(Tool.CHARGENUMBER, chargeNumber);
		}

		m_statistics = new Statistics(task.getJobHandle(), task);
		m_statistics.setResource(getResource());
		m_statistics.setSubmitter(getSubmitter());
		if (chargeNumber != null)
		{
			m_statistics.setTgChargeNumber(chargeNumber);
		}

		m_statisticsEvent = new StatisticsEvent(task.getJobHandle());
		m_statisticsEvent.setEventName("SET_RUNNINGTASK_STATUS");
		m_statisticsEvent.setEventValue("NEW");
	}


	private RunningTask(ResultSet row) throws SQLException, IOException
	{
		super(TABLE_NAME);
		populate(row);
		m_isNew = false;
		getTask();
	}



	// public methods



	public String getResource() { return m_resource.getValue(); }
	public void setResource(String i) { m_resource.setValue(i); }


	public String getToolId() { return m_tool_id.getValue(); }
	public void setToolId(String i) { m_tool_id.setValue(i); }

	public long getUserId() { return m_user_id.getValue(); }
	public void setUserId(long i) { m_user_id.setValue(i); }

	public long getTaskId() { return m_task_id.getValue(); }
	public void setTaskId(long i) { m_task_id.setValue(i); }

	public String getSubmitter() { return m_submitter.getValue(); }
	public void setSubmitter(String i) { m_submitter.setValue(i); }

	public String getRemoteJobId() { return m_remote_job_id.getValue(); }
	public void setRemoteJobId(String i) { m_remote_job_id.setValue(i); }

	public Date getDateEntered() { return m_date_entered.getValue(); }
	//public void setDateSubmitted(Date i) { m_date_entered.setValue(i); }


	public Date getLocked() { return m_locked.getValue(); }
	private void setLocked(Date i) { m_locked.setValue(i); }

	public String getHostname() { return m_hostname.getValue(); }
	private void setHostname(String i) { m_hostname.setValue(i); }

	public long getPid() { return m_pid.getValue(); }
	private void setPid(long i) { m_pid.setValue(i); }

	public String getStatus() { return m_status.getValue(); }

	// Status can only be set via changeStatus().
	//public void setStatus(String i) { m_status.setValue(i); }


	public int getAttempts() { return m_attempts.getValue(); }
	public void setAttempts(int i) { m_attempts.setValue(i); }

	public String getJobhandle() { return m_jobhandle.getValue(); }
	private void setJobhandle(String i) { m_jobhandle.setValue(i); }

	

	public String getOutputDesc() { return m_output_desc.getValue(); }
	public void setOutputDesc(String i) { m_output_desc.setValue(i); }

	
	public String getCommandline() { return m_commandline.getValue(); }
	public void setCommandline(String i) { m_commandline.setValue(i); }

	

	public String getSprops() { return m_sprops.getValue(); }
	public void setSprops(String i) { m_sprops.setValue(i); }

	private Task m_task = null;
	private boolean triedToFindTask = false;
	private Statistics m_statistics; 
	private StatisticsEvent m_statisticsEvent = null; 

	public void setStatisticsEvent(StatisticsEvent se)
	{
		m_statisticsEvent = se;
	}
		

	public Task getTask() throws IOException, SQLException
	{
		if (!triedToFindTask)
		{
			triedToFindTask = true;
			try
			{
				m_task = new Task(m_task_id.getValue());
				if (m_task.getJobHandle() == null || !m_task.getJobHandle().equals(this.getJobhandle()))
				{
					m_task = null;
				}
			}
			catch (NotExistException nee)
			{
			}
		}
		return m_task;
	}


	public Map<String, String> parameters() throws SQLException, IOException
	{
		if (m_parameters == null) {
			Map<String, String> newParameters = new TreeMap<String, String>();

			if (!isNew()) {
				Connection dbConn = ConnectionManager.getConnectionSource().getConnection();
				PreparedStatement selectStmt = null;
				ResultSet prefRows = null;

				try {
					selectStmt = dbConn.prepareStatement("SELECT NAME, VALUE FROM running_tasks_parameters WHERE JOBHANDLE = ?");

					m_jobhandle.setParameter(selectStmt, 1);

					prefRows = selectStmt.executeQuery();

					while (prefRows.next())
						newParameters.put(prefRows.getString(1), prefRows.getString(2));
				}
				finally {
					if (prefRows != null)
						prefRows.close();

					if (selectStmt != null)
						selectStmt.close();

					dbConn.close();
				}
			}

			m_parameters = new ParameterMap(newParameters);
		}

		return m_parameters;
	}

	public boolean isNew()
	{
		return m_isNew; 
	}

	@Override
	public boolean equals(Object other)
	{
		if (other == null)
			return false;

		if (this == other)
			return true;

		if (other instanceof RunningTask == false)
			return false;

		RunningTask otherRunningTask = (RunningTask) other;

		if (isNew() || otherRunningTask.isNew())
			return false;

		return getJobhandle().equals(otherRunningTask.getJobhandle());
	}

	@Override
	public int hashCode()
	{
		return getJobhandle().hashCode();
	}

	public int compareTo(RunningTask other)
	{
		if (other == null)
			throw new NullPointerException("other");

		if (this == other)
			return 0;

		if (isNew())
			return -1;

		if (other.isNew())
			return 1;

		return (int) (getJobhandle().compareTo(other.getJobhandle()));
	}


	// package methods


	Criterion getKey()
	{
		return new SimpleKey(m_jobhandle);
	}


	void save(Connection dbConn) throws IOException, SQLException
	{
		List<Column<?>> colList = new ArrayList<Column<?>>();

		if (isNew()) {
			colList.add(m_resource);
			colList.add(m_tool_id);
			colList.add(m_user_id);
			colList.add(m_task_id);
			colList.add(m_submitter);
			colList.add(m_remote_job_id);
			colList.add(m_date_entered);
			colList.add(m_locked);
			colList.add(m_hostname);
			colList.add(m_pid);
			colList.add(m_status);
			colList.add(m_attempts);
			colList.add(m_jobhandle);
			colList.add(m_output_desc);
			colList.add(m_commandline);
			colList.add(m_sprops);

			insert(dbConn, null, colList);
			m_isNew = false;

		}
		else {
			if (m_resource.isModified())
				colList.add(m_resource);

			if (m_tool_id.isModified())
				colList.add(m_tool_id);

			if (m_user_id.isModified())
				colList.add(m_user_id);

			if (m_task_id.isModified())
				colList.add(m_task_id);

			if (m_submitter.isModified())
				colList.add(m_submitter);

			if (m_remote_job_id.isModified())
				colList.add(m_remote_job_id);

			if (m_date_entered.isModified())
				colList.add(m_date_entered);

			/*
			if (m_locked.isModified())
				colList.add(m_locked);
			if (m_hostname.isModified())
				colList.add(m_hostname);
			if (m_pid.isModified())
				colList.add(m_pid);
			if (m_status.isModified())
				colList.add(m_status);
			*/
			if (m_attempts.isModified())
				colList.add(m_attempts);

			if (m_jobhandle.isModified())
				colList.add(m_jobhandle);

			if (m_output_desc.isModified())
				colList.add(m_output_desc);

			if (m_commandline.isModified())
				colList.add(m_commandline);

			if (m_sprops.isModified())
				colList.add(m_sprops);

			update(dbConn, colList);
		}
		for (Iterator<RowOperation> operations = m_opQueue.iterator() ; operations.hasNext() ; )
			operations.next().execute(dbConn);
		m_opQueue.clear();


		if (m_task != null)
		{
			try
			{
				m_task.save(dbConn);
			}
			catch(NotExistException nee) 
			{ 
				log.debug("TX - Task doesn't exist.");
			}
		}
		if (m_statistics != null)
		{
			m_statistics.save(dbConn);
			m_statistics = null;
		}
		if (m_statisticsEvent != null)
		{
			m_statisticsEvent.save(dbConn);
			m_statisticsEvent = null;
		}
		//log.debug("TX - saved RT and Task, about to end transaction.");
	}

	void delete(Connection dbConn) throws IOException, SQLException
	{
		if (isNew())
			throw new WorkbenchException("Not persisted");
		delete(dbConn, m_jobhandle.getValue());

		if (m_statisticsEvent != null)
		{
			m_statisticsEvent.save(dbConn);
			m_statisticsEvent = null;
		}
		if (m_task != null)
		{
			try
			{
				m_task.setTerminal(true);
				m_task.save(dbConn);
			}
			catch(NotExistException nee) {}
		}

		m_jobhandle.reset();
		m_isNew = true;
	}

	void refresh(Connection dbConn) throws IOException, SQLException
	{
		select(dbConn, COLUMN_NAMES);

		m_opQueue.clear();
		m_parameters = null;
	}


	static void delete(Connection dbConn, String jobhandle) throws SQLException, IOException
	{
		deleteParameters(dbConn, jobhandle);

		PreparedStatement deleteStmt = dbConn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE JOBHANDLE = ?");

		try {
			deleteStmt.setString(1, jobhandle);
			deleteStmt.executeUpdate();

		}
		finally {
			deleteStmt.close();
		}
	}

	public static RunningTask findRunningTaskByTask(long taskId) throws IOException, SQLException
	{
		List<RunningTask> list;
		list = findRunningTask(new LongCriterion("TASK_ID", taskId));
		if (list.size() == 0)
		{
			return null;
		}
		if (list.size() > 1)
		{
			throw new WorkbenchException("Multiple running tasks with same taskId = " + taskId + ".");
		}
		return list.get(0);
	}



	/*
		Find locked records that loadresults or recoverresults would process if they weren't locked.
		Check to see if process that locked them is still running and unlock them if not.
	*/
	public static void unlockResultsReady(String submitter, boolean alreadyTried) throws Exception
	{
		List<RunningTask> list = findResultsReady(submitter, alreadyTried, true);
		unlockBatch(list);
	}

	/*
		Find records that submitJob would process if not locked.  Unlock if process
		that locked them is gone.
	*/
	public static void unlockReadyToSubmit(String submitter, Boolean alreadyTried) throws Exception
	{
		List<RunningTask> list = findReadyToSubmit(submitter, alreadyTried, true);
		unlockBatch(list);
	}

	/*
		Unlock records if the process that holds the lock is no longer running.
		We can only determine that if we're running on the same host that locked the
		record. 
	*/
	private static void unlockBatch(List<RunningTask> list) throws Exception
	{
		String hostname = Workbench.getInstance().getProperties().getProperty("hostname");
		for (RunningTask rt : list)
		{
			log.debug("Checking lock on " + rt.getJobhandle() + ", " + rt.getHostname() + ", " + rt.getPid());
			if (rt.getHostname().equals(hostname))
			{
				if (!ProcessUtils.isRunning(rt.getPid()))
				{
					log.info("Unlocking " + rt.getJobhandle());
					RunningTask.unlock(rt.getJobhandle());
				}
			} else
			{
				// For the time being, since we only run use one host per website, finding records
				// for this website but a different hostname indicates a BIG problem. Todo: how to 
				// have some log messages send email to administrator.
				log.error("Not unlocking because my hostname '" + hostname + 
					"' doesn't match record's.  For submitter: '" + rt.getSubmitter() + "'");
			}
		}
	}

	/*
		This is the query that LoadResults uses to find RT records where
			- submitter =		this web site's url  
			- remote_job_id =	NOT NULL
			- status = 			STATUS_TERMINATED

			loadResults also requires that attemps = 0, while recoverResults requires that attempts > 0
			Called with locked = false to find records that can be locked and processed by loadResults.

			Called with locked = true by unlockWithResultsReady() to find records that
			loadResults or recoverResults would process if they weren't locked. 


	*/
	public static List<RunningTask> findResultsReady(String submitter, boolean alreadyTried, boolean locked)
		throws IOException, SQLException
	{
		/*
			This clause prevents loadResults from processing SSHProcessWorker jobs and jobs
			where submitJobD hasn't had a chance to set remoteJobID yet (see WorkQueue.submitted()).
			Even though we don't need remote job id to load the results we want to give submitJobD 
			a chance to set it so we have it for our tgusage accounting.
		*/
		String clause = " and REMOTE_JOB_ID IS NOT NULL "; 
		return findRunningTaskBySubmitterAndStatus(submitter, RunningTask.STATUS_TERMINATED, alreadyTried, clause, locked);
	}

	/*
		This is the query that submitJob uses.  For the time being it sets alreadyTried = null to indicate
		that it doesn't care about the number of attempts already made.
	*/
	public static List<RunningTask> findReadyToSubmit(String submitter, Boolean alreadyTried, boolean locked)
			throws Exception
	{
		String clause = "";
		return findRunningTaskBySubmitterAndStatus(submitter, STATUS_NEW, alreadyTried, clause, locked);
	}

	/*
		CheckJobs uses this to find all jobs with status = STATUS_SUBMITTED
		Doesn't care about attempts and locked fields.
	*/
	public static List<RunningTask> findRunningTaskBySubmitterAndStatus(String submitter, String status) 
		throws IOException, SQLException
	{
		return findRunningTaskBySubmitterAndStatus(submitter, status, "", null);
	}

	public static List<RunningTask> findRunningTaskBySubmitterAndStatus(
											String submitter, 
											String status, 
											Boolean alreadyTried,
											String clause,
											Boolean locked)
	throws IOException, SQLException
	{
		if (alreadyTried != null)
		{
			if (alreadyTried == true)
			{
				clause += " and (ATTEMPTS !=  0) ";
			} else
			{
				clause += " and (ATTEMPTS = 0 ) ";
			}
		}
		return findRunningTaskBySubmitterAndStatus(submitter, status, clause, locked);
	}

	private static List<RunningTask> findRunningTaskBySubmitterAndStatus(
										String submitter, 
										String status, 
										String clause, 
										Boolean locked) 
	throws IOException, SQLException
	{
		StringBuilder stmtBuilder = new StringBuilder("SELECT " + COLUMN_NAMES + " FROM " + TABLE_NAME + " WHERE ");
		stmtBuilder.append(" SUBMITTER = ? AND STATUS = ? " );

		stmtBuilder.append(clause);
		if (locked != null)
		{
			if (locked == false)
			{
				stmtBuilder.append(andIsNotLocked);
			} else
			{
				stmtBuilder.append(andIsLocked);
			}
		}
		Connection dbConn = ConnectionManager.getConnectionSource().getConnection();
		PreparedStatement selectStmt = null;
		ResultSet runningTaskRows = null;

		try {
			selectStmt = dbConn.prepareStatement(stmtBuilder.toString());
			selectStmt.setString(1, submitter);
			selectStmt.setString(2, status);

			//log.debug("Executing: " + selectStmt.toString());

			runningTaskRows = selectStmt.executeQuery();
			List<RunningTask> retval = new ArrayList<RunningTask>();
			while (runningTaskRows.next())
				retval.add(new RunningTask(runningTaskRows));

			return retval;
		}
		finally {
			if (runningTaskRows != null)
				runningTaskRows.close();

			if (selectStmt != null)
				selectStmt.close();

			dbConn.close();
		}
	}




	static List<RunningTask> findRunningTask(Criterion key) throws IOException, SQLException
	{
		StringBuilder stmtBuilder = new StringBuilder("SELECT " + COLUMN_NAMES + " FROM " + TABLE_NAME + " WHERE ");

		stmtBuilder.append(key.getPhrase());

		Connection dbConn = ConnectionManager.getConnectionSource().getConnection();
		PreparedStatement selectStmt = null;
		ResultSet runningTaskRows = null;

		try {
			selectStmt = dbConn.prepareStatement(stmtBuilder.toString());

			key.setParameter(selectStmt, 1);

			runningTaskRows = selectStmt.executeQuery();

			List<RunningTask> retval = new ArrayList<RunningTask>();

			while (runningTaskRows.next())
				retval.add(new RunningTask(runningTaskRows));

			return retval;
		}
		finally {
			if (runningTaskRows != null)
				runningTaskRows.close();

			if (selectStmt != null)
				selectStmt.close();

			dbConn.close();
		}
	}
	/*
	public void delete() throws IOException, SQLException
	{
		Connection dbConn = ConnectionManager.getConnectionSource().getConnection();

		try {
			dbConn.setAutoCommit(false);

			delete(dbConn);

			dbConn.commit();
		}
		catch (IOException ioErr) {
			dbConn.rollback();
			throw ioErr;
		}
		catch (SQLException sqlErr) {
			dbConn.rollback();
			throw sqlErr;
		}
		finally {
			dbConn.close();
		}
	}
	*/


	public static boolean changeStatus(String jobhandle, String newStatus) throws Exception
	{
		String onlyIf;
		if (newStatus.equals(STATUS_SUBMITTED))
		{
			onlyIf = " AND ((STATUS = 'NEW'))"; 
		} else if (newStatus.equals(STATUS_TERMINATED))
		{
			//if we get curl command to indicate termination before it gets marked submitted status will be new.
			onlyIf = " AND ((STATUS = 'SUBMITTED') || (STATUS = 'NEW'))"; 
		} else
		{
			throw new WorkbenchException("Attempt to set an invalid running task status: " + newStatus);
		}
		StringBuilder stmtBuilder = new StringBuilder();
		stmtBuilder.append("UPDATE ");
		stmtBuilder.append(TABLE_NAME);
		stmtBuilder.append(" SET STATUS = '" + newStatus + "', ATTEMPTS = 0 "); 
		stmtBuilder.append(" WHERE JOBHANDLE = '" + jobhandle + "'");
		stmtBuilder.append(onlyIf);

		Connection dbConn = ConnectionManager.getConnectionSource().getConnection();
		PreparedStatement statement = null;
		try
		{
			statement = dbConn.prepareStatement(stmtBuilder.toString());
			dbConn.setAutoCommit(false);
			int result = statement.executeUpdate();
			if (result == 1)
			{	
				StatisticsEvent se = new StatisticsEvent(jobhandle);
				se.setEventName("SET_RUNNINGTASK_STATUS");
				se.setEventValue(newStatus);
				se.save(dbConn);

				if (newStatus.equals(STATUS_TERMINATED))
				{
					Statistics s = Statistics.find(jobhandle);
					s.setDateTerminated(new Date());
					s.save(dbConn);
				}
			} else if (result == 0)
			{
				log.debug("Attempt to change status of rt " + jobhandle + " to " +
						newStatus + " didn't update anything. Probably current status is > new status or rt has been removed." );
			} else
			{
				throw new WorkbenchException("");
			}
			dbConn.commit();
			return (result == 1) ? true : false;
		} catch (SQLException sqlErr) 
		{
			dbConn.rollback();
			throw sqlErr;
		}
		finally
		{
			if (statement != null) { statement.close(); };
			dbConn.close();
		}
	}


	// ###
	public static boolean lock(String jobhandle) throws Exception
	{
		String hostname = Workbench.getInstance().getProperties().getProperty("hostname");
		long pid = Long.parseLong(Workbench.getInstance().getProperties().getProperty("pid"));

		StringBuilder stmtBuilder = new StringBuilder();

		stmtBuilder.append("UPDATE ");
		stmtBuilder.append(TABLE_NAME);
		stmtBuilder.append(" SET LOCKED = NOW(), ");
		stmtBuilder.append(" HOSTNAME = ? , ");
		stmtBuilder.append(" PID =  + ? " );

		stmtBuilder.append(" WHERE JOBHANDLE = '" + jobhandle + "'");
		stmtBuilder.append(andIsNotLocked);

		Connection dbConn = ConnectionManager.getConnectionSource().getConnection();
		PreparedStatement updateStmt = dbConn.prepareStatement(stmtBuilder.toString());
		try
		{
			dbConn.setAutoCommit(false);
			updateStmt.setString(1, hostname);
			updateStmt.setLong(2, pid);
			int result = updateStmt.executeUpdate(); 
			dbConn.commit();
			return (result == 0) ? false : true;
		}
		catch (SQLException sqlErr) {
			dbConn.rollback();
			throw sqlErr;
		}
		finally
		{
			updateStmt.close();
			dbConn.close();
		}
	}

	/*
		Don't call this unless you're already holding the lock. 
	*/
	public static void unlock(String jobhandle) throws Exception
	{
		StringBuilder stmtBuilder = new StringBuilder();

		stmtBuilder.append("UPDATE ");
		stmtBuilder.append(TABLE_NAME);
		stmtBuilder.append(" SET LOCKED = NULL, ");
		stmtBuilder.append(" HOSTNAME = NULL, ");
		stmtBuilder.append(" PID = NULL ");

		stmtBuilder.append(" WHERE JOBHANDLE = '" + jobhandle + "'");

		Connection dbConn = ConnectionManager.getConnectionSource().getConnection();
		PreparedStatement updateStmt = dbConn.prepareStatement(stmtBuilder.toString());
		try
		{
			dbConn.setAutoCommit(false);
			int result = updateStmt.executeUpdate();
			dbConn.commit();
			if (result == 0)
			{
				/*
				throw new WorkbenchException("Failed to unlock running_task " + jobhandle + 
					".  If there was an error processing the task, this is probably because the running_task " +
					" has already been deleted.");
				*/
			}
		}
		catch (SQLException sqlErr) {
			dbConn.rollback();
			throw sqlErr;
		}
		finally
		{
			updateStmt.close();
			dbConn.close();
		}	
	}

	/*
	*/
	public static long incrementAttempts(String jobhandle, String status) 
		throws SQLException, IOException
    {
        Connection dbConn = ConnectionManager.getConnectionSource().getConnection();
        String statement = "UPDATE " + TABLE_NAME + " SET ATTEMPTS = ATTEMPTS + 1 WHERE JOBHANDLE  = ? and STATUS = ?";
        PreparedStatement ps = null;
        try
        {
            ps = dbConn.prepareStatement(statement);
            dbConn.setAutoCommit(false);

            ps.setString(1, jobhandle);
            ps.setString(2, status);
            int result = ps.executeUpdate();
			if (result != 1)
			{
				log.error("incrementAttemps: expected to update 1 row, but updated " + result);
			}
            dbConn.commit();
        }
        catch (SQLException sqlErr)
        {
            dbConn.rollback();
            throw sqlErr;
        }
        finally
        {
            if (ps != null) { ps.close(); };
            dbConn.close();
        }

		RunningTask rt = RunningTask.find(jobhandle);
		return rt.getAttempts();
    }



	public String rtToString()
    {
        return this.getJobhandle() + 
			", taskId=" + this.getTaskId() + 
			", remoteId=" + this.getRemoteJobId();
    }


	// protected methods


	protected void populate(ResultSet row) throws SQLException, IOException
	{
		m_jobhandle.assignValue(row, 1);
		m_resource.assignValue(row, 2);
		m_tool_id.assignValue(row, 3);
		m_user_id.assignValue(row, 4);
		m_task_id.assignValue(row, 5);
		m_submitter.assignValue(row, 6);
		m_remote_job_id.assignValue(row, 7);
		m_date_entered.assignValue(row, 8);
		m_locked.assignValue(row, 9);
		m_hostname.assignValue(row, 10);
		m_pid.assignValue(row, 11);
		m_status.assignValue(row, 12);
		m_attempts.assignValue(row, 13);
		m_output_desc.assignValue(row, 14);
		m_commandline.assignValue(row, 15);
		m_sprops.assignValue(row, 16);
		m_isNew = false;

	}


	// private methods


	private static void deleteParameters(Connection dbConn, String jobhandle) throws SQLException
	{
		PreparedStatement deleteStmt = dbConn.prepareStatement("DELETE FROM running_tasks_parameters WHERE JOBHANDLE  = ?");

		try {
			deleteStmt.setString(1, jobhandle);

			deleteStmt.executeUpdate();
		}
		finally {
			deleteStmt.close();
		}
	}

}
