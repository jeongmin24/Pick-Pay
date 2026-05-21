package com.ssafy.pickpay.config;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "잘못된 요청입니다");
    	return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(errorResponse);
    }

}
