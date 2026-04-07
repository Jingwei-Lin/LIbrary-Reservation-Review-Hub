package com.comp5619.libraryreservationreviewhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对应表：Waiting_list
 */
@TableName("waiting_list")
@Data
public class WaitingList implements Serializable {

    /** wait_id INT PK AUTO_INCREMENT */
    @TableId(value = "wait_id", type = IdType.AUTO)
    private Long waitId;

    /** user_id INT NOT NULL */
    @TableField("user_id")
    private Long userId;

    /** book_id INT NOT NULL */
    @TableField("book_id")
    private Long bookId;

    /** join_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP */
    @TableField("join_date")
    private LocalDateTime joinDate;

    /** notification_date TIMESTAMP NULL */
    @TableField("notification_date")
    private LocalDateTime notificationDate;

    /** is_notified BOOLEAN DEFAULT FALSE */
    @TableField("is_notified")
    private Boolean isNotified;

    /** status ENUM('waiting','notified','cancelled','fulfilled') DEFAULT 'waiting' */
    @TableField("status")
    private WaitingStatusEnum status;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
