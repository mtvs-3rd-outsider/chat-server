package com.outsider.mop.user.dto;


import lombok.Data;

@Data
public class CustomUserInfoDTO {
    private Long userId;
    private String email;
    private Authority role;
    private String username;

    public CustomUserInfoDTO() {
    }


}
