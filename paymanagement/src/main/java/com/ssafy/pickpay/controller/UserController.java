package com.ssafy.pickpay.controller;

import java.nio.file.AccessDeniedException;
import java.util.Collections;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.pickpay.dto.UserRequestDTO;
import com.ssafy.pickpay.dto.UserResponseDTO;
import com.ssafy.pickpay.service.UserService;

@RestController //json
public class UserController {

	private final UserService userService;

	public UserController(com.ssafy.pickpay.service.UserService userService) {
		super();
		this.userService = userService;
	}
	
	// 유저 존재 확인
	@PostMapping(value = "/user/exist", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> existUserApi(
            @Validated(UserRequestDTO.existGroup.class) @RequestBody UserRequestDTO dto
    ) {
        return ResponseEntity.ok(userService.existUser(dto));
    }
	
	// 회원가입
	@PostMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> joinApi(
            @Validated(UserRequestDTO.addGroup.class) @RequestBody UserRequestDTO dto
    ) {
        Long id = userService.addUser(dto);
        Map<String, Long> responseBody = Collections.singletonMap("userId", id);
        return ResponseEntity.status(201).body(responseBody);
    }
	
	// 유저 정보 
	@GetMapping(value = "/user")
	public UserResponseDTO userMeApi() {
		return userService.readUser();
	}
	
	// 유저 수정
	@PutMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Long> updateUserApi(
			@Validated(UserRequestDTO.updateGroup.class) @RequestBody UserRequestDTO dto 
			) throws AccessDeniedException {
		return ResponseEntity.status(200).body(userService.updateUser(dto)); // userId 반환 
	}
	
	
	// 유저 제거 
	@DeleteMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Boolean> deleteUserApi(
			@Validated(UserRequestDTO.deleteGroup.class) @RequestBody UserRequestDTO dto
			) throws AccessDeniedException {
		userService.deleteUser(dto);
		return ResponseEntity.status(200).body(true);
	}
}
