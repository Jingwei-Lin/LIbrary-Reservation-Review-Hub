package com.comp5619.libraryreservationreviewhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 评论实体类
 */
@TableName(value = "review")
@Data
public class Review implements Serializable {

    /**
     * 评论ID - 主键
     */
    @TableId(value = "review_id", type = IdType.AUTO)
    private Long reviewId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 图书ID
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 评分 (1-5星)
     */
    private Integer rating;

    /**
     * 评论内容
     */
    private String comment;

    /**
     * 评论时间
     */
    @TableField("review_date")
    private Date reviewDate;

    private static final long serialVersionUID = 1L;
}