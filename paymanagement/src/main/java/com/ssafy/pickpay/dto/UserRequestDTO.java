package com.ssafy.pickpay.dto;

// DTO 데이터를 받기전에 Valid 검증 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRequestDTO {
	
	public interface existGroup {} // 회원 가입시 username 존재 확인
    public interface addGroup {} // 회원 가입시
    public interface passwordGroup {} // 비밀번호 변경시
    public interface updateGroup {} // 회원 수정시
    public interface deleteGroup {} // 회원 삭제시
	
    @NotBlank(groups = {existGroup.class, addGroup.class, updateGroup.class, deleteGroup.class}) @Size(min = 4)
	private String loginId;
    @NotBlank(groups = {addGroup.class, passwordGroup.class}) @Size(min = 4)
	private String password;
    @NotBlank(groups = {addGroup.class, updateGroup.class})
	private String nickname;
	
	public String getLoginId() {
		return loginId;
	}

	public void setUserId(String loginId) {
		this.loginId = loginId;
	}
	
	public String getPassword() {
		return password;
	}

	public String getNickname() {
		return nickname;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	
	

}
