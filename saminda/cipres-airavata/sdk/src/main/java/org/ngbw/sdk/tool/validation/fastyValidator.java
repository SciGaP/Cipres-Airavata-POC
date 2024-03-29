package org.ngbw.sdk.tool.validation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import org.ngbw.sdk.api.tool.ParameterValidator2;
import org.ngbw.sdk.api.tool.FieldError;
import org.ngbw.sdk.common.util.BaseValidator;
import org.ngbw.sdk.database.TaskInputSourceDocument;
import org.ngbw.sdk.common.util.SuperString;
public class fastyValidator implements ParameterValidator2
{
private Set<String> requiredParameters;
private Set<String> requiredInput;
private Set<String> allowedParameters;
private Set<String> allowedInput;
// Map of parameter name to list separator (a single character string) for List parameters.
private Map<String, String> listParameters;
// Maps param name to permitted values for List, Excl, Switch type params
private Map<String, List<String>> listValues;
private Map<String, List<String>> exclValues ;
private Map<String, List<String>> switchValues;
private List<FieldError> errors = new ArrayList<FieldError>();
public fastyValidator() {
requiredParameters = new HashSet<String>();
requiredInput = new HashSet<String>();
allowedParameters = new HashSet<String>();
allowedInput = new HashSet<String>();
listParameters = new HashMap<String, String>();
listValues = new HashMap<String, List<String>>();
exclValues = new HashMap<String, List<String>>();
switchValues = new HashMap<String, List<String>>();
List<String> switchList = new ArrayList<String>(2);
switchList.add("0");
switchList.add("1");
List<String> tmpList;
tmpList = new ArrayList<String>();
tmpList.add("fasty_t");
exclValues.put("fasta_", tmpList);
allowedParameters.add("fasta_");
requiredParameters.add("fasta_");
allowedInput.add("query_");
requiredInput.add("query_");
tmpList = new ArrayList<String>();
tmpList.add("DNA");
exclValues.put("seqtype_", tmpList);
allowedParameters.add("seqtype_");
requiredParameters.add("seqtype_");
tmpList = new ArrayList<String>();
tmpList.add("UNIPROT.fasta");
tmpList.add("SWISSPROT.fasta");
tmpList.add("TREMBL.fasta");
tmpList.add("PFILTUNIPROT.fasta");
tmpList.add("PFILTTREMBL.fasta");
tmpList.add("PFILTSWISSPROT.fasta");
tmpList.add("PDBSEQ.fasta");
tmpList.add("REFSEQ_FUNGI_PROTEIN.fasta");
tmpList.add("REFSEQ_INV_PROTEIN.fasta");
tmpList.add("REFSEQ_MICRO_PROTEIN.fasta");
tmpList.add("REFSEQ_MITO_PROTEIN.fasta");
tmpList.add("REFSEQ_PLANT_PROTEIN.fasta");
tmpList.add("REFSEQ_PLASM_PROTEIN.fasta");
tmpList.add("REFSEQ_PLAST_PROTEIN.fasta");
tmpList.add("REFSEQ_PROT_PROTEIN.fasta");
tmpList.add("REFSEQ_VERTM_PROTEIN.fasta");
tmpList.add("REFSEQ_VERTO_PROTEIN.fasta");
tmpList.add("REFSEQ_VIRAL_PROTEIN.fasta");
tmpList.add("TPA_PROTEIN.fasta");
tmpList.add("UNIMES.fasta");
tmpList.add("UNIREF100.fasta");
tmpList.add("NCBI_NR.fasta");
exclValues.put("protein_db_", tmpList);
allowedParameters.add("protein_db_");
allowedParameters.add("break_long_");
allowedParameters.add("ktup_");
allowedParameters.add("optcut_");
allowedParameters.add("gapinit_");
allowedParameters.add("gapext_");
allowedParameters.add("high_expect_");
allowedParameters.add("low_expect_");
allowedParameters.add("dna_match_");
allowedParameters.add("nucleotid_mismatch_");
tmpList = new ArrayList<String>();
tmpList.add("BL50");
tmpList.add("BL62");
tmpList.add("BL80");
tmpList.add("P20");
tmpList.add("P40");
tmpList.add("P120");
tmpList.add("P250");
tmpList.add("M10");
tmpList.add("M20");
tmpList.add("M40");
exclValues.put("matrix_", tmpList);
allowedParameters.add("matrix_");
allowedParameters.add("X_penalty_");
allowedParameters.add("frameshift_");
allowedParameters.add("frameshift_within_");
allowedParameters.add("threeframe_");
switchValues.put("threeframe_", switchList);
allowedParameters.add("invert_");
switchValues.put("invert_", switchList);
tmpList = new ArrayList<String>();
tmpList.add("1");
tmpList.add("2");
tmpList.add("3");
tmpList.add("4");
tmpList.add("5");
tmpList.add("6");
tmpList.add("9");
tmpList.add("10");
tmpList.add("11");
tmpList.add("12");
tmpList.add("13");
tmpList.add("14");
tmpList.add("15");
exclValues.put("genetic_code_", tmpList);
allowedParameters.add("genetic_code_");
allowedParameters.add("band_");
allowedParameters.add("swalig_");
switchValues.put("swalig_", switchList);
allowedParameters.add("noopt_");
switchValues.put("noopt_", switchList);
tmpList = new ArrayList<String>();
tmpList.add("-1");
tmpList.add("0");
tmpList.add("1");
tmpList.add("2");
tmpList.add("3");
tmpList.add("4");
tmpList.add("5");
exclValues.put("stat_", tmpList);
allowedParameters.add("stat_");
allowedParameters.add("random_");
switchValues.put("random_", switchList);
allowedParameters.add("histogram_");
switchValues.put("histogram_", switchList);
allowedParameters.add("scores_");
allowedParameters.add("alns_");
allowedParameters.add("html_output_");
switchValues.put("html_output_", switchList);
tmpList = new ArrayList<String>();
tmpList.add("0");
tmpList.add("1");
tmpList.add("2");
tmpList.add("3");
tmpList.add("4");
tmpList.add("9");
tmpList.add("10");
exclValues.put("markx_", tmpList);
allowedParameters.add("markx_");
allowedParameters.add("init1_");
switchValues.put("init1_", switchList);
tmpList = new ArrayList<String>();
tmpList.add("1");
tmpList.add("0");
exclValues.put("z_score_out_", tmpList);
allowedParameters.add("z_score_out_");
allowedParameters.add("showall_");
switchValues.put("showall_", switchList);
allowedParameters.add("linlen_");
allowedParameters.add("offsets_");
allowedParameters.add("info_");
switchValues.put("info_", switchList);
allowedParameters.add("filter_");
switchValues.put("filter_", switchList);
}
public List<FieldError> getErrors()
{
return errors;
}
public Map<String, String> preProcessParameters(Map<String, List<String>> parameters)
{
Map<String, String> preProcessed = new HashMap<String, String>();
for (String param : parameters.keySet())
{
String newValue;
List<String> values = parameters.get(param);
// Is param of type List?
if (listValues.keySet().contains(param))
{
for (String value : values)
{
List<String> permittedValues = listValues.get(param);
if (!permittedValues.contains(value))
{
errors.add(new FieldError(param, "'" + value + "' is not a permitted value."));
}
}
// concatenate the multiple choices.
String separator = listParameters.get(param);
if (separator != null)
{
newValue = SuperString.valueOf(parameters.get(param), separator.charAt(0)).toString();
} else
{
newValue = SuperString.valueOf(parameters.get(param), '@').toString();
}
} else
{
newValue = parameters.get(param).get(0);
if (parameters.get(param).size() > 1)
{
errors.add(new FieldError(param, "multiple values are not permitted."));
}
if (exclValues.keySet().contains(param))
{
if (!exclValues.get(param).contains(newValue))
{
errors.add(new FieldError(param, "'" + newValue + "' is not a permitted value."));
}
} else if (switchValues.keySet().contains(param))
{
if (!switchValues.get(param).contains(newValue))
{
// todo: error
errors.add(new FieldError(param, "'" + newValue + "' is not a permitted value, must be 0 or 1."));
}
}
}
preProcessed.put(param, newValue);
}
return preProcessed;
}
public void validateParameters(Map<String, String> parameters) {
Set<String> missingRequired = validateRequiredParameters(parameters);
for (String missing : missingRequired)
{
errors.add(new FieldError(missing, "Parameter is required."));
}
for (String param : parameters.keySet())
{
if (!allowedParameters.contains(param))
{
errors.add(new FieldError(param, "Does not exist."));
continue;
}
String error = validate(param, parameters.get(param));
if (error != null)
{
errors.add(new FieldError(param, error));
}
}
}
public void validateInput(Set<String> input) {
Set<String> missingRequired = validateRequiredInput(input);
for (String missing : missingRequired)
{
errors.add(new FieldError(missing, "Input file parameter is required."));
}
}
String validate(String parameter, Object value) {
if (parameter.equals("fasta_")) {
if (BaseValidator.validateString(value) == false)
return "Parameter is required.";
return null;
}
if (parameter.equals("seqtype_")) {
if (BaseValidator.validateString(value) == false)
return "Parameter is required.";
return null;
}
if (parameter.equals("break_long_")) {
if (BaseValidator.validateInteger(value) == false)
return "Must be an integer.";
return null;
}
if (parameter.equals("ktup_")) {
if (BaseValidator.validateInteger(value) == false)
return "Must be an integer.";
return null;
}
if (parameter.equals("optcut_")) {
if (BaseValidator.validateInteger(value) == false)
return "Must be an integer.";
return null;
}
if (parameter.equals("gapinit_")) {
if (BaseValidator.validateInteger(value) == false)
return "Must be an integer.";
return null;
}
if (parameter.equals("gapext_")) {
if (BaseValidator.validateInteger(value) == false)
return "Must be an integer.";
return null;
}
if (parameter.equals("high_expect_")) {
if (BaseValidator.validateDouble(value) == false)
return "Must be a Double.";
return null;
}
if (parameter.equals("low_expect_")) {
if (BaseValidator.validateDouble(value) == false)
return "Must be a Double.";
return null;
}
if (parameter.equals("dna_match_")) {
if (BaseValidator.validateInteger(value) == false)
return "Must be an integer.";
return null;
}
if (parameter.equals("nucleotid_mismatch_")) {
if (BaseValidator.validateInteger(value) == false)
return "Must be an integer.";
return null;
}
if (parameter.equals("X_penalty_")) {
if (BaseValidator.validateInteger(value) == false)
return "Must be an integer.";
return null;
}
if (parameter.equals("frameshift_")) {
if (BaseValidator.validateInteger(value) == false)
return "Must be an integer.";
return null;
}
if (parameter.equals("frameshift_within_")) {
if (BaseValidator.validateInteger(value) == false)
return "Must be an integer.";
return null;
}
if (parameter.equals("band_")) {
if (BaseValidator.validateInteger(value) == false)
return "Must be an integer.";
return null;
}
if (parameter.equals("scores_")) {
if (BaseValidator.validateInteger(value) == false)
return "Must be an integer.";
return null;
}
if (parameter.equals("alns_")) {
if (BaseValidator.validateInteger(value) == false)
return "Must be an integer.";
return null;
}
if (parameter.equals("linlen_")) {
if (BaseValidator.validateInteger(value) == false)
return "Must be an integer.";
return null;
}
return null;
}
private Set<String> validateRequiredParameters(Map<String, String> parameters) {
Set<String> required = new HashSet<String>(requiredParameters.size());
required.addAll(requiredParameters);
for (String parameter : parameters.keySet()) {
if (required.contains(parameter))
required.remove(parameter);
}
return required;
}
private Set<String> validateRequiredInput(Map<String, List<TaskInputSourceDocument>> input) {
Set<String> required = new HashSet<String>(requiredInput.size());
required.addAll(requiredInput);
for (String parameter : input.keySet()) {
if (required.contains(parameter))
required.remove(parameter);
}
return required;
}
private Set<String> validateRequiredInput(Set<String> input) {
Set<String> required = new HashSet<String>(requiredInput.size());
required.addAll(requiredInput);
for (String parameter : input) {
if (required.contains(parameter))
required.remove(parameter);
}
return required;
}
}