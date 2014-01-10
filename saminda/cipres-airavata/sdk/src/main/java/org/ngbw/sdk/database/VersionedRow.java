/*
 * VersionedRow.java
 */
package org.ngbw.sdk.database;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 *
 * @author Paul Hoover
 *
 */
public abstract class VersionedRow extends Row {
	private static final Log log = LogFactory.getLog(VersionedRow.class.getName());

	private final IntegerColumn m_version;


	// constructors


	protected VersionedRow(String tableName)
	{
		super(tableName);

		m_version = new IntegerColumn(tableName + ".VERSION", false);
	}


	// public methods


	public int getVersion()
	{
		return m_version.getValue();
	}


	// protected methods


	protected void populate(ResultSet row, int index) throws IOException, SQLException
	{
		m_version.assignValue(row, index);
	}

	@Override
	protected void update(Connection dbConn, List<Column<?>> colList) throws IOException, SQLException
	{
		Criterion key = getKey();
		int dbVersion = getDbVersion(dbConn, key);
		int memoryVersion = m_version.getValue();

		if (memoryVersion != dbVersion)
			throw new StaleRowException(m_tableName, key, memoryVersion, dbVersion);

		m_version.setValue(memoryVersion + 1);

		// debug only
		/*
		if (m_tableName.equals("tasks"))
		{
			try
			{
				throw new Exception("");
			} catch (Exception e)
			{
				log.debug("Updating version for " + m_tableName + ", key=" + key + " to " + m_version.getValue(), e);
			}
		}
		*/
		// end debug only

		colList.add(m_version);

		super.update(dbConn, colList);
	}

	@Override
	protected void insert(Connection dbConn, AutoGeneratedKey key, List<Column<?>> colList) throws IOException, SQLException
	{
		m_version.setValue(0);

		colList.add(m_version);

		super.insert(dbConn, key, colList);
	}


	// private methods


	private int getDbVersion(Connection dbConn, Criterion key) throws IOException, SQLException
	{
		StringBuilder stmtBuilder = new StringBuilder("SELECT ");

		stmtBuilder.append(m_version.getName());
		stmtBuilder.append(" FROM ");
		stmtBuilder.append(m_tableName);
		stmtBuilder.append(" WHERE ");
		stmtBuilder.append(key.getPhrase());

		PreparedStatement selectStmt = dbConn.prepareStatement(stmtBuilder.toString());
		ResultSet versionRow = null;

		try {
			key.setParameter(selectStmt, 1);

			versionRow = selectStmt.executeQuery();

			if (!versionRow.next())
				return 0;

			return versionRow.getInt(1);
		}
		finally {
			if (versionRow != null)
				versionRow.close();

			selectStmt.close();
		}
	}
}
