package com.onestar.aicc.sample.exception;

import com.onestar.aicc.commons.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * sample 패키지에만 적용되는 예외 처리기.
 * basePackages로 스코프를 한정해 향후 전역 GlobalExceptionHandler(exception 패키지)가
 * 실제 업무 요건으로 추가될 때 충돌하지 않도록 한다.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.onestar.aicc.sample")
public class SampleExceptionHandler {

    @ExceptionHandler(SampleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(SampleNotFoundException e) {
        ErrorResponse body = ErrorResponse.builder()
                .code("SAMPLE_NOT_FOUND")
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ErrorResponse.FieldError.builder()
                        .field(fieldError.getField())
                        .rejectedValue(fieldError.getRejectedValue())
                        .reason(fieldError.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.builder()
                .code("VALIDATION_FAILED")
                .message("요청 값이 올바르지 않습니다.")
                .errors(fieldErrors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e) {
        ErrorResponse body = ErrorResponse.builder()
                .code("INVALID_REQUEST_BODY")
                .message("요청 본문을 읽을 수 없습니다. JSON 형식과 인코딩(UTF-8)을 확인하세요.")
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("sample 패키지에서 처리되지 않은 예외가 발생했습니다.", e);
        ErrorResponse body = ErrorResponse.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("서버 내부 오류가 발생했습니다.")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
