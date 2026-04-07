package com.comp5619.libraryreservationreviewhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comp5619.libraryreservationreviewhub.model.entity.BookRating;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BookRatingMapper extends BaseMapper<BookRating> {
    @Select("CALL update_book_rating(#{bookId})")
    void recalcOne(@Param("bookId") Long bookId);
}
