package hu.telcodata.integration.constant;

public final class MessageConstants {

    public static final String CSV_EMPTY = "The CSV file contains no data rows";
    public static final String CSV_PARSE_ERROR = "The CSV file could not be parsed";
    public static final String JSON_EMPTY = "The JSON file contains no records";
    public static final String JSON_PARSE_ERROR = "The JSON file could not be parsed";
    public static final String INPUT_FILE_REQUIRED = "An input file is required";
    public static final String INPUT_FILE_READ_ERROR = "The input file could not be read";
    public static final String SHA_256_UNAVAILABLE = "SHA-256 is not available";
    public static final String NO_COMPLETED_PROCESSING_RUN = "No completed processing run exists";
    public static final String INVALID_REQUEST = "Invalid request";
    public static final String PRIMARY_SOURCE_REQUIRED = "A primary source must be selected";
    public static final String MALFORMED_REQUEST_BODY = "The request body is missing or malformed";
    public static final String MISSING_REQUEST_PART = "Required request part is missing: %s";
    public static final String MISSING_REQUEST_PARAMETER = "Required request parameter is missing: %s";
    public static final String INVALID_REQUEST_PARAMETER = "Invalid value for request parameter %s: %s";
    public static final String REQUEST_METHOD_NOT_SUPPORTED = "The requested HTTP method is not supported";
    public static final String MEDIA_TYPE_NOT_SUPPORTED = "The requested media type is not supported";
    public static final String REQUEST_TOO_LARGE = "The uploaded request is too large";
    public static final String RESOURCE_NOT_FOUND = "The requested resource was not found";
    public static final String DATA_CONFLICT = "The request could not be completed because of a data conflict";
    public static final String UNEXPECTED_SERVER_ERROR = "Unexpected server error";
    public static final String UNSUPPORTED_SORT_FIELD = "Unsupported sort field: %s";
    public static final String UNSUPPORTED_SORT_DIRECTION = "Unsupported sort direction: %s";
    public static final String NEGATIVE_PAGE = "Page must not be negative";
    public static final String INVALID_PAGE_SIZE = "Page size must be between 1 and 100";
    public static final String PROCESSING_RUN_NOT_FOUND = "Processing run not found: %s";
    public static final String UPLOAD_BATCH_NOT_FOUND = "Upload batch not found: %s";
    public static final String BATCH_WRONG_SOURCE = "Batch %s is not a %s batch";
    public static final String BATCH_NOT_READY = "Batch %s is not ready for processing";

    public static final String MISSING_SKU = "The source record has no SKU";
    public static final String DUPLICATE_PRIMARY_SOURCE = "The primary source contains multiple records for the same SKU";
    public static final String DUPLICATE_SECONDARY_SOURCE = "The secondary source contains multiple records for the same SKU";
    public static final String SOURCE_ONLY_RECORD = "The record exists in only one source";
    public static final String FALLBACK_VALUE_USED = "The value was filled from the secondary source";
    public static final String SOURCE_CONFLICT = "Primary source value differs from the secondary source";
    public static final String MISSING_REQUIRED_FIELD = "A required value is missing";
    public static final String SOURCE_VALUE_CONVERSION_ERROR = "The source value could not be converted to HUF";
    public static final String INVALID_NUMERIC_VALUE = "Invalid numeric value in %s: %s";
    public static final String INVALID_INTEGER_VALUE = "Invalid integer value in %s: %s";

    private MessageConstants() {
    }

    public static String invalidNumericValue(String field, String value) {
        return INVALID_NUMERIC_VALUE.formatted(field, value);
    }

    public static String invalidIntegerValue(String field, String value) {
        return INVALID_INTEGER_VALUE.formatted(field, value);
    }

    public static String unsupportedSortField(String field) {
        return UNSUPPORTED_SORT_FIELD.formatted(field);
    }

    public static String unsupportedSortDirection(String direction) {
        return UNSUPPORTED_SORT_DIRECTION.formatted(direction);
    }

    public static String processingRunNotFound(Object runId) {
        return PROCESSING_RUN_NOT_FOUND.formatted(runId);
    }

    public static String uploadBatchNotFound(Object batchId) {
        return UPLOAD_BATCH_NOT_FOUND.formatted(batchId);
    }

    public static String batchWrongSource(Object batchId, Object sourceType) {
        return BATCH_WRONG_SOURCE.formatted(batchId, sourceType);
    }

    public static String missingRequestPart(String partName) {
        return MISSING_REQUEST_PART.formatted(partName);
    }

    public static String missingRequestParameter(String parameterName) {
        return MISSING_REQUEST_PARAMETER.formatted(parameterName);
    }

    public static String invalidRequestParameter(String parameterName, Object value) {
        return INVALID_REQUEST_PARAMETER.formatted(parameterName, value);
    }

}
