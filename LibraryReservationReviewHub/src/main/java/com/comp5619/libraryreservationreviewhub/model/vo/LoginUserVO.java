package com.comp5619.libraryreservationreviewhub.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 已登录用户视图（脱敏）
 */
@Data
public class LoginUserVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * email (userAccount)
     */
    private String userAccount;

    /**
     * first_name
     */
    private String firstName;

    /**
     * last_name
     */
    private String lastName;

    /**
     * computed userName
     */
    private String userName;

    /**
     * is_admin boolean (true/false)
     */
    private Boolean isAdmin;

    /**
     * derived userRole
     */
    private String userRole;

    /**
     * phone
     */
    private String phone;

    /**
     * create_time
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}