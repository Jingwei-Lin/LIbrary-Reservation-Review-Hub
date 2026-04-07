package com.comp5619.libraryreservationreviewhub.model.dto.book;

import lombok.Data;

import java.io.Serializable;

/**
 * 评论添加请求
 */
@Data
public class ReviewAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 评分 (1-5星)
     */
    private Integer rating;

    /**
     * 评论内容
     */
    private String content;

}