package com.comp5619.libraryreservationreviewhub.model.dto.reservation;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 预订查询请求
 */
@Data
public class ReservationQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 预订ID
     */
    private Long reservationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 预订状态
     */
    private String status;

    /**
     * 开始日期
     */
    private Date startDate;

    /**
     * 结束日期
     */
    private Date endDate;

    /**
     * 当前页号
     */
    private long current = 1;

    /**
     * 页面大小
     */
    private long pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序
     */
    private String sortOrder;
}