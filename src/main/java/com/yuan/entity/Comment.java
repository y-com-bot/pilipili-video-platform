package com.yuan.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    private Long id;
    private Long videoId;
    private Long userId;
    private String content;
    private Date createTime;
    private Integer likeCount;
}
