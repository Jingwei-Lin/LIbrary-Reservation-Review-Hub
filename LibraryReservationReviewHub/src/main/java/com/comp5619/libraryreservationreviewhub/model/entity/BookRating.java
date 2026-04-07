package com.comp5619.libraryreservationreviewhub.model.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.math.BigDecimal;

@Data
public class BookRating {
    @TableId("book_id")
    private Long bookId;

    @TableField("avg_rating")
    private BigDecimal avgRating;
}