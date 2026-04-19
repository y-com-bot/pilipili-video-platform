package com.yuan.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Video {
    private Long id;
    private String title;
    private String description;
    private Long uploaderId;
    private String videoUrl;
    private Date createTime;
    private Integer likeCount;
}