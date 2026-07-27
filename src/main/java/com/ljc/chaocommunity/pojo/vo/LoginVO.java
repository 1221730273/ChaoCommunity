package com.ljc.chaocommunity.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginVO {


    //TODO 以后可以把用户的id返回回去？方便一些前端的业务处理

    private String token;

}