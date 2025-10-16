package com.infomoney.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleDTO {

    private String title;
    private String subtitle;
    private String author;
    private String url;
    private String content;
    private LocalDateTime publicationDate;
}
