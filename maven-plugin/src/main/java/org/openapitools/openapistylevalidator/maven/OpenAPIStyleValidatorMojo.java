package org.openapitools.openapistylevalidator.maven;

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openapitools.empoa.swagger.core.internal.SwAdapter;
import org.openapitools.openapistylevalidator.OpenApiSpecStyleValidator;
import org.openapitools.openapistylevalidator.ValidatorParameters;
import org.openapitools.openapistylevalidator.ValidatorParameters.NamingConvention;
import org.openapitools.openapistylevalidator.styleerror.StyleError;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

@Mojo(name = "validate", defaultPhase = LifecyclePhase.VERIFY)
public class OpenAPIStyleValidatorMojo extends AbstractMojo {

    @Parameter(property = OpenApiSpecStyleValidator.INPUT_FILE)
    private String inputFile;

    @Parameter(property = ValidatorParameters.VALIDATE_INFO_LICENSE, defaultValue = "true")
    private boolean validateInfoLicense = true;

    @Parameter(property = ValidatorParameters.VALIDATE_INFO_DESCRIPTION, defaultValue = "true")
    private boolean validateInfoDescription = true;

    @Parameter(property = ValidatorParameters.VALIDATE_INFO_CONTACT, defaultValue = "true")
    private boolean validateInfoContact = true;

    @Parameter(property = ValidatorParameters.VALIDATE_SERVER_INFO, defaultValue = "true")
    private boolean validateServerInfo = true;

    @Parameter(property = ValidatorParameters.VALIDATE_OPERATION_OPERATION_ID, defaultValue = "true")
    private boolean validateOperationOperationId = true;

    @Parameter(property = ValidatorParameters.VALIDATE_OPERATION_OPERATION_ID_UNIQUE, defaultValue = "true")
    private boolean validateOperationOperationIdUnique = true;

    @Parameter(property = ValidatorParameters.VALIDATE_OPERATION_DESCRIPTION, defaultValue = "true")
    private boolean validateOperationDescription = true;

    @Parameter(property = ValidatorParameters.VALIDATE_OPERATION_TAG, defaultValue = "true")
    private boolean validateOperationTag = true;

    @Parameter(property = ValidatorParameters.VALIDATE_OPERATION_SUMMARY, defaultValue = "true")
    private boolean validateOperationSummary = true;

    @Parameter(property = ValidatorParameters.VALIDATE_MODEL_PROPERTIES_EXAMPLE, defaultValue = "true")
    private boolean validateModelPropertiesExample = true;

    @Parameter(property = ValidatorParameters.VALIDATE_MODEL_NO_LOCAL_DEF, defaultValue = "true")
    private boolean validateModelNoLocalDef = true;

    @Parameter(property = ValidatorParameters.VALIDATE_NAMING, defaultValue = "true")
    private boolean validateNaming = true;

    @Parameter(property = ValidatorParameters.IGNORE_HEADER_X_NAMING, defaultValue = "true")
    private boolean ignoreHeaderXNaming = true;

    @Parameter(property = ValidatorParameters.PATH_NAMING_CONVENTION, defaultValue = "HyphenCase")
    private NamingConvention pathNamingConvention = NamingConvention.HyphenCase;

    @Parameter(property = ValidatorParameters.PARAMETER_NAMING_CONVENTION, defaultValue = "CamelCase")
    private NamingConvention parameterNamingConvention = NamingConvention.CamelCase;

    @Parameter(property = ValidatorParameters.PROPERTY_NAMING_CONVENTION, defaultValue = "CamelCase")
    private NamingConvention propertyNamingConvention = NamingConvention.CamelCase;

    @Parameter(property = ValidatorParameters.UPDATED_SERVER_URL_TLDS)
    private String[] updatedServerUrlTLDs;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (inputFile == null) {
            throw new MojoExecutionException(String.format("The input file is not defined, set the '%s' option", OpenApiSpecStyleValidator.INPUT_FILE));
        }
        getLog().info(String.format("Validating spec: %s", inputFile));

        OpenAPIParser openApiParser = new OpenAPIParser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);

        SwaggerParseResult parserResult = openApiParser.readLocation(inputFile, null, parseOptions);
        io.swagger.v3.oas.models.OpenAPI swaggerOpenAPI = parserResult.getOpenAPI();

        org.eclipse.microprofile.openapi.models.OpenAPI openAPI = SwAdapter.toOpenAPI(swaggerOpenAPI);
        OpenApiSpecStyleValidator openApiSpecStyleValidator = new OpenApiSpecStyleValidator(openAPI);

        ValidatorParameters parameters = createValidatorParameters();
        getLog().debug(String.format("Validating with options: %s", parameters));
        List<StyleError> result = openApiSpecStyleValidator.validate(parameters);
        if (!result.isEmpty()) {
            getLog().error("OpenAPI Specification does not meet the requirements. Issues:\n");
            result.stream().map(StyleError::toString).forEach(m -> getLog().error(String.format("\t%s", m)));
            throw new MojoExecutionException("OpenAPI Style validation failed");
        }
    }

    public ValidatorParameters createValidatorParameters() {
        ValidatorParameters parameters = new ValidatorParameters();
        parameters.setValidateInfoLicense(validateInfoLicense);
        parameters.setValidateInfoDescription(validateInfoDescription);
        parameters.setValidateInfoContact(validateInfoContact);
        parameters.setValidatorServerInfo(validateServerInfo);
        parameters.setValidateOperationOperationId(validateOperationOperationId);
        parameters.setValidateOperationOperationId(validateOperationOperationIdUnique);
        parameters.setValidateOperationDescription(validateOperationDescription);
        parameters.setValidateOperationTag(validateOperationTag);
        parameters.setValidateOperationSummary(validateOperationSummary);
        parameters.setValidateModelPropertiesExample(validateModelPropertiesExample);
        parameters.setValidateModelNoLocalDef(validateModelNoLocalDef);
        parameters.setValidateNaming(validateNaming);
        parameters.setIgnoreHeaderXNaming(ignoreHeaderXNaming);
        parameters.setPathNamingConvention(pathNamingConvention);
        parameters.setParameterNamingConvention(parameterNamingConvention);
        parameters.setPropertyNamingConvention(propertyNamingConvention);
        parameters.setUpdatedServerUrlTLDs(updatedServerUrlTLDs);
        return parameters;
    }
}
