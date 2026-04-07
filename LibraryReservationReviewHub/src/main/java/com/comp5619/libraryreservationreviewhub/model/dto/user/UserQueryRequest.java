package com.comp5619.libraryreservationreviewhub.model.dto.user;


import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * email (userAccount)
     */
    private String userAccount;

    /**
     * first_name (searchable)
     */
    private String firstName;

    /**
     * last_name (searchable)
     */
    private String lastName;

    /**
     * phone (searchable)
     */
    private String phone;

    /**
     * is_admin (searchable, maps to userRole)
     */
    private Boolean isAdmin;

    private static final long serialVersionUID = 1L;
}