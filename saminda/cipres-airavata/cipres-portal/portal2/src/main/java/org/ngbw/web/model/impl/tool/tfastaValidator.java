package org.ngbw.web.model.impl.tool;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ngbw.sdk.api.tool.ParameterValidator;
import org.ngbw.sdk.common.util.BaseValidator;
import org.ngbw.sdk.database.TaskInputSourceDocument;
public class tfastaValidator implements ParameterValidator
{
private Set<String> requiredParameters;
private Set<String> requiredInput;
public tfastaValidator() {
requiredParameters = new HashSet<String>();
requiredInput = new HashSet<String>();
requiredParameters.add("fasta_");
requiredParameters.add("seqtype_");
}
public Map<String, String> validateParameters(Map<String, String> parameters) {
Map<String, String> errors = new HashMap<String, String>();
Set<String> missingRequired = validateRequiredParameters(parameters);
for (String missing : missingRequired) {
errors.put(missing, "You must enter a value for \"" + missing + "\"");
}
for (String param : parameters.keySet()) {
String error = validate(param, parameters.get(param));
if (error != null)
errors.put(param, error);
}
return errors;
}
public Map<String, String> validateInput(Map<String, List<TaskInputSourceDocument>> input) {
Map<String, String> errors = new HashMap<String, String>();
Set<String> missingRequired = validateRequiredInput(input);
for (String missing : missingRequired) {
errors.put(missing, "You must enter a value for \"" + missing + "\"");
}
for (String param : input.keySet()) {
String error = validate(param, input.get(param));
if (error != null)
errors.put(param, error);
}
return errors;
}
public String validate(String parameter, Object value) {
if (parameter.equals("fasta_")) {
if (BaseValidator.validateString(value) == false)
return "You must enter a value for \"" + parameter + "\"";
return null;
}
if (parameter.equals("seqtype_")) {
if (BaseValidator.validateString(value) == false)
return "You must enter a value for \"" + parameter + "\"";
return null;
}
if (parameter.equals("break_long_")) {
if (BaseValidator.validateInteger(value) == false)
return "\"" + parameter + "\" must be an integer.";
return null;
}
if (parameter.equals("ktup_")) {
if (BaseValidator.validateInteger(value) == false)
return "\"" + parameter + "\" must be an integer.";
return null;
}
if (parameter.equals("optcut_")) {
if (BaseValidator.validateInteger(value) == false)
return "\"" + parameter + "\" must be an integer.";
return null;
}
if (parameter.equals("gapinit_")) {
if (BaseValidator.validateInteger(value) == false)
return "\"" + parameter + "\" must be an integer.";
return null;
}
if (parameter.equals("gapext_")) {
if (BaseValidator.validateInteger(value) == false)
return "\"" + parameter + "\" must be an integer.";
return null;
}
if (parameter.equals("high_expect_")) {
if (BaseValidator.validateDouble(value) == false)
return "\"" + parameter + "\" must be a Double.";
return null;
}
if (parameter.equals("low_expect_")) {
if (BaseValidator.validateDouble(value) == false)
return "\"" + parameter + "\" must be a Double.";
return null;
}
if (parameter.equals("dna_match_")) {
if (BaseValidator.validateInteger(value) == false)
return "\"" + parameter + "\" must be an integer.";
return null;
}
if (parameter.equals("nucleotid_mismatch_")) {
if (BaseValidator.validateInteger(value) == false)
return "\"" + parameter + "\" must be an integer.";
return null;
}
if (parameter.equals("X_penalty_")) {
if (BaseValidator.validateInteger(value) == false)
return "\"" + parameter + "\" must be an integer.";
return null;
}
if (parameter.equals("frameshift_")) {
if (BaseValidator.validateInteger(value) == false)
return "\"" + parameter + "\" must be an integer.";
return null;
}
if (parameter.equals("frameshift_within_")) {
if (BaseValidator.validateInteger(value) == false)
return "\"" + parameter + "\" must be an integer.";
return null;
}
if (parameter.equals("band_")) {
if (BaseValidator.validateInteger(value) == false)
return "\"" + parameter + "\" must be an integer.";
return null;
}
if (parameter.equals("scores_")) {
if (BaseValidator.validateInteger(value) == false)
return "\"" + parameter + "\" must be an integer.";
return null;
}
if (parameter.equals("alns_")) {
if (BaseValidator.validateInteger(value) == false)
return "\"" + parameter + "\" must be an integer.";
return null;
}
if (parameter.equals("linlen_")) {
if (BaseValidator.validateInteger(value) == false)
return "\"" + parameter + "\" must be an integer.";
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
}