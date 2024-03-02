package studybackend.refrigeratorcleaner.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    EXIST_USER_EMAIL_SOCIALTYPE(HttpStatus.CONFLICT, "해당 socialType의 이메일이 이미 존재합니다."),
    EXIST_USER_NICKNAME(HttpStatus.CONFLICT, "해당 닉네임이 이미 존재합니다."),
    NO_EXIST_USER_EMAIL_SOCIALTYPE(HttpStatus.NOT_FOUND, "해당 socialType의 이메일이 존재하지 않습니다."),
    NO_EXIST_USER_REFRESHTOKEN(HttpStatus.NOT_FOUND, "refreshToken이 없습니다."),
    NO_EXIST_USER_EMAIL(HttpStatus.NOT_FOUND, "이메일이 존재하지않습니다."),
    NO_EXIST_USER_SOCIALID(HttpStatus.NOT_FOUND, "socialId가 존재하지않습니다."),
    NO_EXTRACT_EMAIL(HttpStatus.BAD_REQUEST, "토큰에서 email을 추출 할 수 없습니다.(잘못된 토큰)"),
    NO_EQUAL_JSON(HttpStatus.BAD_REQUEST, "data content-type이 json이 아닙니다."),
    NO_EXTRACT_ACCESSTOKEN(HttpStatus.BAD_REQUEST, "토큰에서 AccessToken을 추출 할 수 없습니다.(잘못된 토큰)"),
    WRONG_CERTIFICATION_NUMBER(HttpStatus.BAD_REQUEST,"인증번호가 틀렸습니다."),
    EXPIRE_CERTIFICATION_NUMBER(HttpStatus.BAD_REQUEST,"인증번호가 만료되었습니다."),
    SEND_EMAIL_FAIL(HttpStatus.INTERNAL_SERVER_ERROR,"이메일 전송을 실패했습니다."),
    NO_AUTHENTICATION_INFO(HttpStatus.BAD_REQUEST,"사용자 정보가 없습니다.")
    ;


    private final HttpStatus httpStatus;
    private final String message;
}