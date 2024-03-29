<%@ taglib prefix="s" uri="/struts-tags" %>
<title>Extractseq</title>
<h2>Extractseq: Extract a region from a sequence (<a href="#REFERENCE">Gary Williams</a>)</h2>
<s:form action="extractseq" theme="simple">
<!-- Begin Simple Parameters -->
<a href="javascript:simple.slideit()" class="panel">Simple Parameters</a>
<div id="simple" style="width: 100%; background-color: #FFF;">
<div style="padding: 0.75em 0 0 0">
<A HREF="javascript:help.slidedownandjump('#regions_option')">Enter the regions to extract, separated by commas (-regions)</A>
<font color="red" size="3">*</font>
<s:textfield name="regions_option_" size="10" maxlength="600" onchange="resolveParameters()"/>
<br/>
<A HREF="javascript:help.slidedownandjump('#upload_rangefile')">Upload a file with ranges in it</A>
<s:checkbox name="upload_rangefile_" onclick="resolveParameters()"/>
<br/>
Select the file that contains the ranges you wish to select
<s:select name="input_rangefile_" headerKey='' headerValue='' list="%{ getDataForParameter('input_rangefile_')}" onchange="resolveParameters()"/>
<br/>
<A HREF="javascript:help.slidedownandjump('#write_separate')">Write Regions a separate sequences in one file (-separate) </A>
<s:checkbox name="write_separate_" onclick="resolveParameters()"/>
<br/>
Output format for output
<s:select name="outseq_sformat_" headerKey='' headerValue='' list="#{ 'fasta':'fasta' , 'gcg':'gcg' , 'embl':'embl' , 'swiss':'swiss' , 'ncbi':'ncbi' , 'nbrf':'nbrf' , 'genbank':'genbank' , 'ig':'ig' , 'text':'text' , 'asn1':'asn1' }" onchange="resolveParameters()"/>
<br/>
<br/>
</div>
</div>
<script type="text/javascript">
var simple=new animatedcollapse("simple", 800, false, "block")
</script>
<!--End Simple Parameters -->
<br/><hr/><br/>
<!--Begin Advanced Parameters -->
<a href="javascript:advanced.slideit()" class="panel">Advanced Parameters</a>
<div id="advanced" style="width: 100%; background-color: #FFF;">
<div style="padding: 0.75em 0 0 0">
</div>
</div>
<script type="text/javascript">
var advanced=new animatedcollapse("advanced", 800, true)
</script>
<!--End Advanced Parameters -->
<br/><hr/><br/>
<s:submit value="Save Parameters" onclick="return validateControl()"/>
<s:submit value="Reset" method="resetPage"/>
<s:submit value="Cancel" method="cancel"/>
<hr></hr>
<!--Begin Advanced Help -->
<a href="javascript:help.slideit()" class="panel">Advanced Help</a>
<div id="help" style="width: 100%; background-color: #FFF;">
<div style="padding: 0.75em 0 0 0">
<dt><a name=regions_option><i>Enter the regions to extract, separated by commas (-regions)</i></a></dt>
<dd>Regions to be extracted are specified by a numerical pairs, eg 45-67, where the two numbers specify the starting and ending positions to be extracted. Each range must be specified by integers. If you wish to extract multiple sequences in a range, you may separate them by commas, as follows: 24-45, 56-78. Other non alphanumeric characters can be used. The following are valid entries: 1:45, 67=99;765..888 1,5,8,10,23,45,57,9</dd>
<dt><a name=upload_rangefile><i>Upload a file with ranges in it</i></a></dt>
<dd>The format of the range file is as follows: Comment lines start with '#' in the first column. Comment lines and blank lines are ignored.
The line may start with white-space. There are two positive (integer) numbers per line separated by one or more space or TAB characters.
The second number must be greater or equal to the first number. There can be optional text after the two numbers to annotate the line.
White-space before or after the text is removed.</dd>
<dt><a name=write_separate><i>Write Regions a separate sequences in one file (-separate) </i></a></dt>
<dd>This option causes all output to be written to separate files</dd>
</div>
</div>
<script type="text/javascript">
var help=new animatedcollapse("help", 800, true)
</script>
<!--End Advanced Help -->
</s:form>
<script type="text/javascript">
function resolveParameters() {
// regions_option
if (!getValue('upload_rangefile_'))
enable('regions_option_');
else disable('regions_option_');
// upload_rangefile
// input_rangefile
if (getValue('upload_rangefile_'))
enable('input_rangefile_');
else disable('input_rangefile_');
// write_separate
// outseq_sformat
}
function validateControl() {
// regions_option
// upload_rangefile
// input_rangefile
// write_separate
// outseq_sformat
return issueWarning();
}
function issueWarning() {
// regions_option
// upload_rangefile
// input_rangefile
// write_separate
// outseq_sformat
return true;
}
function messageSplit(str)
{
var tokens = str.split(" ");
var newStr = ""
var tmp;
for (i = 0; i < tokens.length; i++)
{
if ((tokens[i].indexOf("getValue(") == 0))
{
tmp = tokens[i];
var tmp1, tmp2;
var closeParen = tmp.indexOf(")");
tmp1 = tmp.substring(0, closeParen + 1);
if ((closeParen + 1) == tmp.length)
{
tmp = tmp1 + " + ' '";
} else
{
tmp2=tmp.substring(closeParen + 1);
tmp = tmp1 + " + '" + tmp2 + "'";
tmp = tmp + " + ' '";
}
} else
{
tmp = "'" + tokens[i] + " '";
}
if (newStr.length > 0)
{
newStr = newStr + " + " + tmp;
} else
{
newStr = tmp;
}
}
return eval(newStr);
}
function getValue(parameter) {
var element = document.forms['extractseq'].elements[parameter];
if (element == null)
return null;
// if the element has a length, it's either a drop-down list or a radio button
else if (element.length != null) {
// if the element has a value, it's a drop-down list
if (element.value != null)
return element.value;
// otherwise it's a radio button
else for (i=0; element.length>i; i++) {
if (element[i].checked)
return element[i].value;
}
return null;
} else if (element.type == 'checkbox')
return element.checked;
else return element.value;
}
function enable(parameter) {
var element = document.forms['extractseq'].elements[parameter];
if (element == null)
return;
// if the element has a length, itss either a drop-down list or a radio button
else if (element.length != null) {
// if the element has a value, it's a drop-down list
if (element.value != null)
element.disabled = false;
// otherwise its a radio button
else for (i=0; element.length>i; i++) {
element[i].disabled = false;
}
} else element.disabled = false;
}
function disable(parameter) {
var element = document.forms['extractseq'].elements[parameter];
if (element == null)
return;
// if the element has a length, its either a drop-down list or a radio button
else if (element.length != null) {
// if the element has a value, its a drop-down list
if (element.value != null)
element.disabled = true;
// otherwise its a radio button
else for (i=0; element.length>i; i++) {
element[i].disabled = true;
}
} else element.disabled = true;
}
</script>