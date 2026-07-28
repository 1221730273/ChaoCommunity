package com.ljc.chaocommunity.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginInfo {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private Integer role;

    private Integer status;
}
