package com.ssafy.pickpay.config;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

//예외처리 클래스 
@RestControllerAdvice
public class CustomControllerAdvice { 

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
    	Map<String, String> errorResponse = new HashMap<>();
    	errorResponse.put("message", ex.getMessage());
    	
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN) // 403
                .body(errorResponse);
    }
    
    // 지원하지 않는 HTTP 메서드 
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", "지원하지 않는 HTTP 메서드입니다.");
        errorResponse.put("requestedMethod", ex.getMethod());
        errorResponse.put("supportedMethods", ex.getSupportedMethods());
        errorResponse.put("path", request.getRequestURI());

        Set<HttpMethod> supportedHttpMethods = ex.getSupportedHttpMethods();

        if (supportedHttpMethods != null && !supportedHttpMethods.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.METHOD_NOT_ALLOWED) // 405
                    .allow(supportedHttpMethods.toArray(new HttpMethod[0]))
                    .body(errorResponse);
        }

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED) // 405
                .body(errorResponse);
    }
    
    // 런타임 에러 
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "잘못된 요청입니다");
    	return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(errorResponse);
    }
    
    // 유효성 검증 실패 에러 
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
    	Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", "입력값이 올바르지 않습니다.");
    	Map<String, String> fieldErrors = new HashMap<>();
    	for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage()); // key: 필드명 / value: 오류메시지
        }
    	errorResponse.put("errors", fieldErrors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllException(Exception ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "서버 내부에서 알 수 없는 오류가 발생했습니다.");
        // 서버 콘솔에 에러 trace 남김
        ex.printStackTrace(); 
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR) // 500 에러
                .body(errorResponse);
    }

}
