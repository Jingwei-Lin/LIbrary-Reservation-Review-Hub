package com.comp5619.libraryreservationreviewhub.model.dto.user;

import lombok.Data;
import java.util.Date;

/**
 * User Update Request DTO
 * Used for updating user profile information (firstName, lastName, email, phone, password)
 */
@Data
public class UserUpdateRequest {
    /**
     * User ID (required for identifying which user to update)
     */
    private Long id;

    /**
     * First name (required)
     */
    private String firstName;

    /**
     * Last name (required)
     */
    private String lastName;

    /**
     * Email address (userAccount, required, must be unique)
     */
    private String email;

    /**
     * Phone number (optional)
     */
    private String phone;

    /**
     * New password (optional, leave blank to keep current password)
     */
    private String password;

    /**
     * Confirm password (required if password is provided)
     */
    private String confirmPassword;

    /**
     * Admin status (optional, typically only updatable by super admins)
     */
    private Boolean isAdmin;

    /**
     * User status (1 = active, 0 = inactive, typically admin-only)
     */
    private Integer status;

    /**
     * Creation time (read-only, typically not updatable)
     */
    private Date createTime;

    /**
     * Last update time (automatically managed by database)
     */
    private Date updateTime;
}