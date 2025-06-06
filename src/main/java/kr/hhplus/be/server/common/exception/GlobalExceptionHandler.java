package kr.hhplus.be.server.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        log.error("[API ERROR] [{}] {}", ex.getErrorCode().getCode(), ex.getMsg());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");   // json 형식으로 리턴

        ErrorResponse response = new ErrorResponse(ex.getErrorCode(), ex.getMsg());

        return ResponseEntity
                .status(response.getStatus())
                .headers(headers)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        log.error("[UNHANDLED ERROR]", ex);
        ApiException apiException = new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
        return handleApiException(apiException);
    }

}
