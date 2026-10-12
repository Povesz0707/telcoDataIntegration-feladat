package hu.telcodata.integration.web;

import hu.telcodata.integration.constant.MessageConstants;
import hu.telcodata.integration.api.ApiErrorResponseDto;
import hu.telcodata.integration.exception.BadRequestException;
import hu.telcodata.integration.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponseDto> handleBadRequest(BadRequestException exception,
                                                              HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponseDto> handleNotFound(ResourceNotFoundException exception,
                                                           HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDto> handleValidation(MethodArgumentNotValidException exception,
                                                             HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; ", "", ""));
        if (message.isBlank()) {
            message = MessageConstants.INVALID_REQUEST;
        }
        return response(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponseDto> handleBinding(BindException exception,
                                                          HttpServletRequest request) {
        String message = exception.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; ", "", ""));
        return response(HttpStatus.BAD_REQUEST,
                message.isBlank() ? MessageConstants.INVALID_REQUEST : message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponseDto> handleUnreadableRequest(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, MessageConstants.MALFORMED_REQUEST_BODY, request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponseDto> handleMissingRequestPart(
            MissingServletRequestPartException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST,
                MessageConstants.missingRequestPart(exception.getRequestPartName()), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponseDto> handleMissingRequestParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST,
                MessageConstants.missingRequestParameter(exception.getParameterName()), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponseDto> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST,
                MessageConstants.invalidRequestParameter(exception.getName(), exception.getValue()), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponseDto> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; ", "", ""));
        return response(HttpStatus.BAD_REQUEST,
                message.isBlank() ? MessageConstants.INVALID_REQUEST : message, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponseDto> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED,
                MessageConstants.REQUEST_METHOD_NOT_SUPPORTED, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponseDto> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                MessageConstants.MEDIA_TYPE_NOT_SUPPORTED, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponseDto> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, MessageConstants.REQUEST_TOO_LARGE, request);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiErrorResponseDto> handleResourceNotFound(
            Exception exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, MessageConstants.RESOURCE_NOT_FOUND, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponseDto> handleDataConflict(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, MessageConstants.DATA_CONFLICT, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseDto> handleUnexpected(Exception exception,
                                                             HttpServletRequest request) {
        log.error("Unhandled server exception: method={}, uri={}", request.getMethod(), request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, MessageConstants.UNEXPECTED_SERVER_ERROR, request);
    }

    private ResponseEntity<ApiErrorResponseDto> response(HttpStatus status, String message,
                                                      HttpServletRequest request) {
        if (status.is4xxClientError()) {
            log.warn("Client or business error response: method={}, uri={}, status={}, message={}",
                    request.getMethod(), request.getRequestURI(), status.value(), message);
        }
        return ResponseEntity.status(status).body(new ApiErrorResponseDto(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
