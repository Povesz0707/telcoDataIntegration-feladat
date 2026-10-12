package hu.telcodata.integration.web;

import hu.telcodata.integration.api.ApiErrorResponseDto;
import hu.telcodata.integration.constant.MessageConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest(HttpMethod.GET.name(), "/api/test");
    }

    @Test
    void mapsMalformedJsonToBadRequest() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "invalid json", new IllegalArgumentException("invalid json"));

        assertError(handler.handleUnreadableRequest(exception, request),
                HttpStatus.BAD_REQUEST, MessageConstants.MALFORMED_REQUEST_BODY);
    }

    @Test
    void mapsMissingMultipartFileToBadRequest() {
        MissingServletRequestPartException exception = new MissingServletRequestPartException("file");

        assertError(handler.handleMissingRequestPart(exception, request),
                HttpStatus.BAD_REQUEST, MessageConstants.missingRequestPart("file"));
    }

    @Test
    void mapsInvalidQueryParameterToBadRequest() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "not-a-number", Integer.class, "page", null, new NumberFormatException());

        assertError(handler.handleTypeMismatch(exception, request),
                HttpStatus.BAD_REQUEST, MessageConstants.invalidRequestParameter("page", "not-a-number"));
    }

    @Test
    void mapsUnsupportedMethodAndMediaType() {
        HttpRequestMethodNotSupportedException methodException =
                new HttpRequestMethodNotSupportedException("PUT");
        HttpMediaTypeNotSupportedException mediaTypeException =
                new HttpMediaTypeNotSupportedException(MediaType.APPLICATION_XML_VALUE);

        assertError(handler.handleMethodNotSupported(methodException, request),
                HttpStatus.METHOD_NOT_ALLOWED, MessageConstants.REQUEST_METHOD_NOT_SUPPORTED);
        assertError(handler.handleMediaTypeNotSupported(mediaTypeException, request),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, MessageConstants.MEDIA_TYPE_NOT_SUPPORTED);
    }

    @Test
    void mapsOversizedUploadAndDataConflict() {
        assertError(handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(100), request),
                HttpStatus.PAYLOAD_TOO_LARGE, MessageConstants.REQUEST_TOO_LARGE);
        assertError(handler.handleDataConflict(new DataIntegrityViolationException("duplicate"), request),
                HttpStatus.CONFLICT, MessageConstants.DATA_CONFLICT);
    }

    @Test
    void mapsMissingRequestParameterAndUnknownRoute() {
        MissingServletRequestParameterException parameterException =
                new MissingServletRequestParameterException("runId", "Long");
        NoHandlerFoundException routeException = new NoHandlerFoundException(
                "GET", "/missing", new HttpHeaders());

        assertError(handler.handleMissingRequestParameter(parameterException, request),
                HttpStatus.BAD_REQUEST, MessageConstants.missingRequestParameter("runId"));
        assertError(handler.handleResourceNotFound(routeException, request),
                HttpStatus.NOT_FOUND, MessageConstants.RESOURCE_NOT_FOUND);
    }

    private void assertError(org.springframework.http.ResponseEntity<ApiErrorResponseDto> response,
                             HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(status.value());
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }
}
