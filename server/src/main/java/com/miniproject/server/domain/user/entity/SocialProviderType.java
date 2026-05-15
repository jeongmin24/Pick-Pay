package com.miniproject.server.domain.user.entity;

import lombok.Getter;
/**
 * enum: "열거형" 자료형
 * {} 안에 가능한 상수값 (고정된 이름)을 나열. 각 값들은 하나의 객체처럼 동작
 * */
@Getter
public enum SocialProviderType {

    NAVER("네이버"),
    GOOGLE("구글");

    private final String description; // enum 설명

    SocialProviderType(String description) {
        this.description = description;
    }
}
