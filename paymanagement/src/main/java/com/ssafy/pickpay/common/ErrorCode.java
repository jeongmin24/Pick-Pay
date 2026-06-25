package com.ssafy.pickpay.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	
	ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 주문만 결제할 수 있습니다."),

    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_ALREADY_PAID(HttpStatus.OK, "이미 결제 완료된 주문입니다."),
    PAYMENT_CONFLICT(HttpStatus.CONFLICT, "이미 처리 중이거나 처리된 결제입니다."),
    PAYMENT_EXPIRED(HttpStatus.GONE, "결제 가능 시간이 만료되었습니다."),

    OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),

    PG_CONFIRM_REJECTED(HttpStatus.BAD_REQUEST, "PG 결제 승인이 거절되었습니다."),
    PG_CONFIRM_FAILED(HttpStatus.BAD_GATEWAY, "PG 결제 승인 중 오류가 발생했습니다."),
    PG_CANCEL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "결제 자동 취소에 실패했습니다."),

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");


	private final HttpStatus status;
	private final String message;
}
