package com.yuan.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class User {
    private Long id;
    private String username;
    private String password;
    private String salt;
    private String role;
    private Date createTime;
}
