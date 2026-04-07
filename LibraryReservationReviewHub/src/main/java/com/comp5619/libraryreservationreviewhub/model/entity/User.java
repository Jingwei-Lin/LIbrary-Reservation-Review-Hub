package com.comp5619.libraryreservationreviewhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * 用户
 * @TableName user
 */
@TableName(value = "Users")
@Data
public class User implements Serializable {
    /**
     * id
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long id;

    /**
     * email (used as userAccount/username)
     */
    @TableField("email")
    private String userAccount;

    /**
     * password
     */
    @TableField("password")
    private String userPassword;

    /**
     * first_name
     */
    @TableField("first_name")
    private String firstName;

    /**
     * last_name
     */
    @TableField("last_name")
    private String lastName;

    /**
     * is_admin (maps to userRole: true = "admin", false = "user")
     */
    @TableField("is_admin")
    private Boolean isAdmin;

    
    /**
     * phone
     */
    @TableField("phone")
    private String phone;

    /**
     * 1表示可用 0表示禁用
     */
    private Integer status;

    /**
     * create time
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;


    /**
     * update
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;


}