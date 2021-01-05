package org.openapitools.openapistylevalidator;

import org.eclipse.microprofile.openapi.models.PathItem;
import org.openapitools.openapistylevalidator.styleerror.*;

import java.util.ArrayList;
import java.util.List;

class ErrorAggregator {

    private final List<StyleError> errorList = new ArrayList<>();

    void logMissingOrEmptyAttribute(StyleError.StyleCheckSection styleCheckSection, String fieldNames) {
        errorList.add(new StyleError(styleCheckSection,
                fieldNames,
                "Should be present and not empty"));
    }

    void validateMinimumInfo(List<Boolean> infoPresence,
                                     StyleError.StyleCheckSection styleCheckSection,
                                     String parentObjectName,
                                     String fieldNames) {
        boolean hasMinimumInfo = false;
        for (Boolean presence : infoPresence) {
            if (presence) {
                hasMinimumInfo = true;
                break;
            }
        }

        if (!hasMinimumInfo) {
            errorList.add(new GenericStyleError(styleCheckSection,
                    parentObjectName,
                    fieldNames,
                    "At least one field should be present and not empty"));
        }
    }

    List<StyleError> getErrorList() {
        return errorList;
    }

    void logMissingOrEmptyOperationAttribute(String path, PathItem.HttpMethod method, String field) {
        errorList.add(new OperationStyleError(field,
                "This field should be present and not empty",
                path, method));
    }

    void logMissingOrEmptyOperationCollection(String path, PathItem.HttpMethod method, String field) {
        errorList.add(new OperationStyleError(field,
                "The collection should be present and there should be at least one item in it",
                path, method));
    }

    void logMissingOrEmptyModelAttribute(String modelName, String propertyName, String field) {
        errorList.add(new ModelStyleError(
                field,
                "This field should be present and not empty",
                modelName, propertyName));
    }

    void logOperationBadNaming(String variableName, String variableType, String neededNamingStrategy, String path, PathItem.HttpMethod httpMethod) {
        errorList.add(new OperationNamingStyleError(StyleError.StyleCheckSection.Naming, variableName,
                String.format("%s should be in %s", variableType, neededNamingStrategy), path, httpMethod));
    }

    void logOperationNonUniqueNaming(String operationName, String operationId, String path, PathItem.HttpMethod httpMethod) {
        errorList.add(new OperationNamingStyleError(StyleError.StyleCheckSection.Naming, operationName,
                String.format("%s should be UNIQUE", operationId), path, httpMethod));
    }

    void logModelBadNaming(String variableName, String variableType, String neededNamingStrategy, String model) {
        errorList.add(new ModelNamingStyleError(StyleError.StyleCheckSection.Naming, variableName,
                String.format("%s should be in %s", variableType, neededNamingStrategy), model));
    }

    void logMissingOrEmptyServerAttribute(String url, String attribute) {
        errorList.add(new StyleError(StyleError.StyleCheckSection.APIInfo, "server", String.format("server url has missing attribute {%s}", attribute)));
    }
}
