package com.comp5619.libraryreservationreviewhub.common;

import lombok.Data;

import java.io.Serializable;

/**
 * common delete request
 * 通用删除请求类
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}

