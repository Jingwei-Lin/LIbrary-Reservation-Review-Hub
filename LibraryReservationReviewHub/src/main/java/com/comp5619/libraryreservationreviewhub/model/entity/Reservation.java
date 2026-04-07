package com.comp5619.libraryreservationreviewhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预订实体类
 */
@TableName(value = "Reservation")
@Data
public class Reservation implements Serializable {
    /**
     * ID
     */
    @TableId(value = "reservation_id")
    private Long reservationId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 图书ID
     */
    @TableId(value = "book_id")
    private Long bookId;

    /**
     * 预订时间
     */
    @TableId(value = "reservation_date")
    private Date reservationTime;

    /**
     * 取书时间
     */

    @TableId(value = "pickup_date")
    private Date pickupTime;

    /**
     * 到期时间
     */
    @TableId(value = "due_date")
    private Date dueTime;

    /**
     * 归还时间
     */
    @TableId(value = "return_date")
    private Date returnTime;

    /**
     * 预订状态：reserved/borrowed/returned/overdue/cancelled
     */
    @TableId(value = "status")
    private String reservationStatus;

    /**
     * 创建时间
     */
    @TableId(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableId(value = "update_time")
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    @TableId(value = "is_delete")
    private Boolean isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}