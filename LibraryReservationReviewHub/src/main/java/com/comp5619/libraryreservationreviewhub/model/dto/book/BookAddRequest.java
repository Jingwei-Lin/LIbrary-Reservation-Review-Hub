package com.comp5619.libraryreservationreviewhub.model.dto.book;

import lombok.Data;

import java.io.Serializable;

/**
 * 图书添加请求
 */
@Data
public class BookAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 书名
     */
    private String title;

    /**
     * 作者
     */
    private String author;

    /**
     * 图书类别 (genre)
     */
    private String genre;

    /**
     * 库存数量
     */
    private Integer quantity;

    /**
     * 是否可借 (默认 true)
     */
    private Boolean available;

    /**
     * 描述
     */
    private String description;

    /**
    * 路径
    */
    private String path;
}
