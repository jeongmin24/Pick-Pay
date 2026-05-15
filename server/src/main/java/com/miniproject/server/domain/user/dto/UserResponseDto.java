package com.miniproject.server.domain.user.dto;

/**
 * record = 데이터를 담기위한 클래스(DTO)를 간결하게 만들자
 * */
public record UserResponseDto (
        String username,
        Boolean Social,
        String nickname,
        String email
) {}
