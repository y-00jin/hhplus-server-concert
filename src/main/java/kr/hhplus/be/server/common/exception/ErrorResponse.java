package kr.hhplus.be.server.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ErrorResponse {

    private final int code;
    private final String title;
    private final HttpStatus status;
    private final String msg;

    public ErrorResponse(ErrorCode errorCode, String msg) {
        this.code = errorCode.getCode();
        this.title = errorCode.getTitle();
        this.status = errorCode.getStatus();
        this.msg = (msg != null && !msg.isEmpty()) ? msg : errorCode.getTitle();
    }
}
