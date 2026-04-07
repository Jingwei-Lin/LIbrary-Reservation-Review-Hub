package com.comp5619.libraryreservationreviewhub.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 图书视图对象 - 修正字段映射
 */
@Data
public class BookVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图书ID - 改为Integer以匹配Book实体
     */
    private Long bookId;

    /**
     * 书名
     */
    private String title;

    /**
     * 作者
     */
    private String author;

    /**
     * 图书类别 - 改为genre以匹配Book实体
     */
    private String genre;

    /**
     * 总库存
     */
    private Integer quantity;

    /**
     * on held quantity for waiting list
     */
    private Integer heldQuantity;

    /**
     * 是否可用
     */
    private Boolean available;

    /**
     * 封面路径
     */
    private String path;

    /**
     * description
     */
    private String description;

}