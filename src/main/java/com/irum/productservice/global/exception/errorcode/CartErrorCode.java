package com.irum.productservice.global.exception.errorcode;

import com.irum.global.advice.exception.errorcode.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CartErrorCode implements BaseErrorCode {
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니 아이템을 찾을 수 없습니다."),
    CART_EXPIRED(HttpStatus.BAD_REQUEST, "장바구니 정보가 만료되었습니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "유효하지 않은 수량 요청입니다."),
    CART_UNAUTHORIZED(HttpStatus.FORBIDDEN, "장바구니 접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String errorClassName() {
        return this.name();
    }
}
