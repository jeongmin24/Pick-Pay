package com.miniproject.server.api;

import com.miniproject.server.domain.user.dto.UserRequestDto;
import com.miniproject.server.domain.user.dto.UserResponseDto;
import com.miniproject.server.domain.user.service.UserService;
import org.apache.coyote.Response;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaTray;
import java.awt.*;
import java.nio.file.AccessDeniedException;
import java.util.Collections;
import java.util.Map;

@RestController
public class UserController {

    private final UserService userService;


    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 자체 로그인 유저 존재 확인
    @PostMapping(value = "/user/exist", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> existUserApi(
            @Validated(UserRequestDto.existGroup.class) @RequestBody UserRequestDto dto
    ) {
        return ResponseEntity.ok(userService.existUser(dto));
    }

    // 회원가입
    @PostMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> joinApi(
            @Validated(UserRequestDto.addGroup.class) @RequestBody UserRequestDto dto
    ) {
        Long id = userService.addUser(dto);
        Map<String, Long> responseBody = Collections.singletonMap("UserEntityId", id);
        return ResponseEntity.status(201).body(responseBody);
    }

    // 유저정보
    @GetMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserResponseDto userMeApi() {
        return userService.readUser();
    }

    // 유저 수정 (자체로그인 유저)
    @PutMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> updateUserApi(
        @Validated(UserRequestDto.updateGroup.class) @RequestBody UserRequestDto dto
    ) throws AccessDeniedException {
        return ResponseEntity.status(200).body(userService.updateUser(dto));
    }

    // 유저 제거 (자체/소셜)
    @DeleteMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> deleteUserApi(
            @Validated(UserRequestDto.deleteGroup.class) @RequestBody UserRequestDto dto
    ) throws AccessDeniedException {

        userService.deleteUser(dto);
        return ResponseEntity.status(200).body(true);
    }
}
