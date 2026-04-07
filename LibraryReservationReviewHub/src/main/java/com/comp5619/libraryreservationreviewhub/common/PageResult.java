package com.comp5619.libraryreservationreviewhub.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private long current;
    private long pageSize;
    private long total;
    private List<T> records;
}
