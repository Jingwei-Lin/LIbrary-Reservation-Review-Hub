package com.comp5619.libraryreservationreviewhub.mapper;

import com.comp5619.libraryreservationreviewhub.model.entity.Genre;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GenreMapper {

    @Select("SELECT * FROM genre WHERE id = #{id}")
    Genre selectById(Long id);

    @Select("SELECT * FROM genre ORDER BY sortOrder ASC")
    List<Genre> selectAll();
}
