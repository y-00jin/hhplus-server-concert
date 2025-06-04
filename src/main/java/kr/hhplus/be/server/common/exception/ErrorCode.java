package kr.hhplus.be.server.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 400 Bad Request
    INVALID_INPUT_VALUE(40001, "잘못된 입력 값입니다", HttpStatus.BAD_REQUEST),
    MISSING_REQUEST_PARAMETER(40002, "요청 파라미터가 누락되었습니다", HttpStatus.BAD_REQUEST),

    // 403 Forbidden,
    FORBIDDEN(40300, "접근 권한이 없습니다", HttpStatus.FORBIDDEN),

    // 404 Not Found
    RESOURCE_NOT_FOUND(40400, "요청한 리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(40500, "허용되지 않은 HTTP 메서드입니다", HttpStatus.METHOD_NOT_ALLOWED),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(50000, "서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    DB_ERROR(50001, "데이터베이스 처리 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),

    // 기타
    UNKNOWN_ERROR(99999, "알 수 없는 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR)
    ;

    private final int code;
    private final String title;
    private final HttpStatus status;

    ErrorCode(int code, String title, HttpStatus status) {
        this.code = code;
        this.title = title;
        this.status = status;
    }
}
