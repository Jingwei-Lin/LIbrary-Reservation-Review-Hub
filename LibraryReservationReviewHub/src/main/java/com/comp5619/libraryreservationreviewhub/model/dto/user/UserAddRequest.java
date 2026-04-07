package com.comp5619.libraryreservationreviewhub.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户创建请求
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * email (used as userAccount/username)
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
     * phone
     */
    private String phone;

    /**
     * is_admin (maps to userRole: true = "admin", false = "user")
     */
    private Boolean isAdmin;

    private static final long serialVersionUID = 1L;

    public boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
}