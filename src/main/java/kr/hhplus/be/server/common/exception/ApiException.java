package kr.hhplus.be.server.common.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String msg;

    public ApiException(ErrorCode errorCode, String msg) {
        super(msg != null ? msg : errorCode.getTitle());
        this.errorCode = errorCode;
        this.msg = msg != null ? msg : errorCode.getTitle();
    }
}
