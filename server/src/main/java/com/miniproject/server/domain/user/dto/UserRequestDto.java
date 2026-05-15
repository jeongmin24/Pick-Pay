package com.miniproject.server.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequestDto {

    public interface existGroup {} // 회원 가입시 username 존재 확인
    public interface addGroup {} // 회원 가입시
    public interface passwordGroup {} // 비밀번호 변경시
    public interface updateGroup {} // 회원 수정시
    public interface deleteGroup {} // 회원 삭제시

    @NotBlank(groups = {existGroup.class, addGroup.class, updateGroup.class, deleteGroup.class}) @Size(min = 4)
    private String username;

    @NotBlank(groups = {addGroup.class, passwordGroup.class}) @Size(min = 4)
    private String password;

    // 빈값을 받지 않도록
    @NotBlank(groups = {addGroup.class, updateGroup.class})
    private String nickname;

    // 이메일 형태가 맞는지 검증
    @Email(groups = {addGroup.class, updateGroup.class})
    private String email;

}
