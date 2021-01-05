package org.openapitools.openapistylevalidator;

import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.openapitools.openapistylevalidator.styleerror.StyleError;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenApiSpecStyleValidator {
    public static final String INPUT_FILE = "inputFile";

    private final OpenAPI openAPI;
    private final ErrorAggregator errorAggregator;
    private ValidatorParameters parameters;
    private final NamingValidator namingValidator;

    public OpenApiSpecStyleValidator(OpenAPI openApi) {
        this.openAPI = openApi;
        errorAggregator = new ErrorAggregator();
        namingValidator = new NamingValidator();
    }

    public List<StyleError> validate(ValidatorParameters parameters) {
        this.parameters = parameters;
        validateInfo();
        validateServers();
        validateOperations();
        validateModels();
        validateNaming();

        return errorAggregator.getErrorList();
    }
    
    private void validateServers() {
        List<Server> servers = openAPI.getServers();
        if (servers != null && parameters.isValidateServerInfo()) {

            String[] updateServerTLDs = parameters.getUpdatedServerUrlTLDs();
            if(updateServerTLDs != null && updateServerTLDs.length > 0){
                DomainValidator.updateTLDOverride(DomainValidator.ArrayType.GENERIC_PLUS, updateServerTLDs);
            }

            Pattern p = Pattern.compile("\\{(.*?)\\}");

            servers.forEach(server -> {
                errorAggregator.getErrorList().add(new StyleError(StyleError.StyleCheckSection.APIInfo, "server", "server:" + server));
                StringBuffer sb = new StringBuffer();
                Matcher m = p.matcher(server.getUrl());
                while(m.find()) {
                    String parameterName = m.group(1);
                    ServerVariable serverVariable = server.getVariables() != null?
                                                        server.getVariables().getServerVariable(parameterName):null;
                    if(serverVariable == null){
                        errorAggregator.logMissingOrEmptyServerAttribute(server.getUrl(), parameterName);
                    }
                    else {
                        m.appendReplacement(sb, serverVariable.getDefaultValue());
                    }
                }
                m.appendTail(sb);
                String url = sb.toString();
                UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
                if (!"/".equals(url) && !urlValidator.isValid(url)) {
                    errorAggregator.getErrorList().add(new StyleError(StyleError.StyleCheckSection.APIInfo, "server", "Invalid server url format:" + url));
                }
            });
        }
    }

    private void validateInfo() {
        Info info = openAPI.getInfo();
        License license = info.getLicense();
        if (parameters.isValidateInfoLicense()) {
            if (license != null) {
                List<Boolean> infoPresence = new ArrayList<>();
                infoPresence.add(license.getName() != null && !license.getName().isEmpty());
                infoPresence.add(license.getUrl() != null && !license.getUrl().isEmpty());
                errorAggregator.validateMinimumInfo(infoPresence, StyleError.StyleCheckSection.APIInfo, "license", "name|url");
            } else {
                errorAggregator.logMissingOrEmptyAttribute(StyleError.StyleCheckSection.APIInfo, "license");
            }
        }

        if (parameters.isValidateInfoDescription()) {
            String description = info.getDescription();
            if (description == null || description.isEmpty()) {
                errorAggregator.logMissingOrEmptyAttribute(StyleError.StyleCheckSection.APIInfo, "description");
            }
        }

        if (parameters.isValidateInfoContact()) {
            Contact contact = info.getContact();
            if (contact != null) {
                List<Boolean> infoPresence = new ArrayList<>();
                infoPresence.add(contact.getName() != null && !contact.getName().isEmpty());
                infoPresence.add(contact.getUrl() != null && !contact.getUrl().isEmpty());
                infoPresence.add(contact.getEmail() != null && !contact.getEmail().isEmpty());
                errorAggregator.validateMinimumInfo(infoPresence, StyleError.StyleCheckSection.APIInfo, "contact", "name|url|email");
            } else {
                errorAggregator.logMissingOrEmptyAttribute(StyleError.StyleCheckSection.APIInfo, "contact");
            }
        }
    }

    private void validateOperations() {
        Set<String> usedOperationIds = new HashSet<>();
        for (String key : openAPI.getPaths().getPathItems().keySet()) {
            PathItem path = openAPI.getPaths().getPathItems().get(key);
            for (PathItem.HttpMethod method : path.getOperations().keySet()) {
                Operation op = path.getOperations().get(method);
                if (parameters.isValidateOperationOperationId()) {
                    if (op.getOperationId() == null || op.getOperationId().isEmpty()) {
                        errorAggregator.logMissingOrEmptyOperationAttribute(key, method, "operationId");
                    }
                }

                if(parameters.isValidateOperationOperationIdUnique()){
                    if(usedOperationIds.contains(op.getOperationId())){
                        errorAggregator.logOperationNonUniqueNaming("operationId", op.getOperationId(), key, method);
                    }
                    else{
                        usedOperationIds.add(op.getOperationId());
                    }
                }

                if (parameters.isValidateOperationDescription()) {
                    if (op.getDescription() == null || op.getDescription().isEmpty()) {
                        errorAggregator.logMissingOrEmptyOperationAttribute(key, method, "description");
                    }
                }

                if (parameters.isValidateOperationSummary()) {
                    if (op.getSummary() == null || op.getSummary().isEmpty()) {
                        errorAggregator.logMissingOrEmptyOperationAttribute(key, method, "summary");
                    }
                }

                if (parameters.isValidateOperationTag()) {
                    if (op.getTags() == null || op.getTags().isEmpty()) {
                        errorAggregator.logMissingOrEmptyOperationCollection(key, method, "tags");
                    }
                }
            }
        }
    }

    private void validateModels() {
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            for (String definition : openAPI.getComponents().getSchemas().keySet()) {
                Schema model = openAPI.getComponents().getSchemas().get(definition);

                if (model.getProperties() != null) {
                    for (Map.Entry<String, Schema> entry : model.getProperties().entrySet()) {
                        Schema property = entry.getValue();

                        if (parameters.isValidateModelPropertiesExample()) {
                            if (property.getRef() == null && property.getExample() == null) {
                                errorAggregator.logMissingOrEmptyModelAttribute(definition, entry.getKey(), "example");
                            }
                        }
                        
                        /*
                    if (parameters.isValidateModelNoLocalDef()) {
                        //TODO:
                    }*/
                    }
                }
            }
        }
    }

    private void validateNaming() {
        if (parameters.isValidateNaming()) {
            if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
                for (String definition : openAPI.getComponents().getSchemas().keySet()) {
                    Schema model = openAPI.getComponents().getSchemas().get(definition);

                    if (model.getProperties() != null) {
                        for (Map.Entry<String, Schema> entry : model.getProperties().entrySet()) {
                            boolean isValid = namingValidator.isNamingValid(entry.getKey(), parameters.getPropertyNamingConvention());
                            if (!isValid) {
                                errorAggregator.logModelBadNaming(entry.getKey(),
                                        "property",
                                        parameters.getPropertyNamingConvention().getDesignation(),
                                        definition);
                            }
                        }
                    }
                }
            }

            if (openAPI.getPaths() != null && openAPI.getPaths().getPathItems() != null) {
                for (String key : openAPI.getPaths().getPathItems().keySet()) {
                    PathItem path = openAPI.getPaths().getPathItems().get(key);
                    for (PathItem.HttpMethod method : path.getOperations().keySet()) {
                        Operation op = path.getOperations().get(method);
                        if (op != null && op.getParameters() != null) {
                            for (Parameter opParam : op.getParameters()) {
                                boolean shouldValidate;
                                if (opParam.getIn() == Parameter.In.HEADER && opParam.getName().startsWith("X-")) {
                                    shouldValidate = !parameters.isIgnoreHeaderXNaming();
                                } else {
                                    shouldValidate = true;
                                }

                                if (shouldValidate && opParam.getRef() == null) {
                                    boolean isValid = namingValidator.isNamingValid(opParam.getName(), parameters.getParameterNamingConvention());
                                    if (!isValid) {
                                        errorAggregator.logOperationBadNaming(opParam.getName(),
                                                "parameter",
                                                parameters.getParameterNamingConvention().getDesignation(),
                                                key,
                                                method);
                                    }
                                }
                            }
                        }
                    }

                    String[] pathParts = key.split("/");
                    for (String part : pathParts) {
                        if (!part.isEmpty() && !(part.startsWith("{") && part.endsWith("}"))) {
                            boolean isValid = namingValidator.isNamingValid(part, parameters.getPathNamingConvention());
                            if (!isValid) {
                                errorAggregator.logOperationBadNaming(part,
                                        "path",
                                        parameters.getPathNamingConvention().getDesignation(),
                                        key,
                                        null);
                            }
                        }
                    }
                }
            }
        }
    }
}