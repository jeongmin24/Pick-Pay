package com.ssafy.pickpay.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ssafy.pickpay.common.ErrorCode;
import com.ssafy.pickpay.dto.ErrorResponse;
import com.ssafy.pickpay.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

//예외처리 클래스 
@Slf4j
@RestControllerAdvice
public class CustomControllerAdvice { 

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(ErrorCode.ACCESS_DENIED));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }
    
    // 지원하지 않는 HTTP 메서드 
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        Set<HttpMethod> supportedHttpMethods = ex.getSupportedHttpMethods();
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);

        if (supportedHttpMethods != null && !supportedHttpMethods.isEmpty()) {
            builder.allow(supportedHttpMethods.toArray(new HttpMethod[0]));
        }

        return builder.body(ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED));
    }
    
    // 런타임 에러 
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled runtime exception", ex);

    	return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
    
    // 유효성 검증 실패 에러 
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
    	Map<String, String> fieldErrors = new HashMap<>();
    	for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage()); // key: 필드명 / value: 오류메시지
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, fieldErrors));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllException(Exception ex) {
        log.error("Unhandled exception", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }

}
