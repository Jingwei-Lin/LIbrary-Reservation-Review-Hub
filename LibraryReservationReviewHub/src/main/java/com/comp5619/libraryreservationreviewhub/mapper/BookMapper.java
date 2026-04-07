package com.comp5619.libraryreservationreviewhub.mapper;

import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BookMapper {

    /* ================= 基础 CRUD ================= */

    /** 按 ID 查询：加入 description；去掉无意义的 ORDER BY */
    @Select("""
        SELECT book_id AS bookId, title, author, genre, description, quantity, held_quantity, available, path
        FROM book
        WHERE book_id = #{id}
        """)
    Book selectById(@Param("id") Long id);

    @Select("""
        SELECT COUNT(1)
        FROM book
        WHERE title = #{title} AND author = #{author} AND genre = #{genre}
        """)
    long countByTag(@Param("title") String title,
                    @Param("author") String author,
                    @Param("genre") String genre);


    @Insert("""
        INSERT INTO book (title, author, genre, description, quantity, held_quantity, available, path)
        VALUES (#{title}, #{author}, #{genre}, #{description}, #{quantity}, 0, #{available}, #{path})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "bookId", keyColumn = "book_id")
    int insertBook(Book b);

    /** 动态更新：仅更新非空字段，补充 description 与 path */
    @Update("""
        <script>
        UPDATE book
        <set>
          <if test="title         != null">title         = #{title},</if>
          <if test="author        != null">author        = #{author},</if>
          <if test="genre         != null">genre         = #{genre},</if>
          <if test="description   != null">description   = #{description},</if>
          <if test="quantity      != null">quantity      = #{quantity},</if>
          <if test="available     != null">available     = #{available},</if>
          <if test="path          != null">path          = #{path},</if>
        </set>
        WHERE book_id = #{bookId}
        </script>
        """)
    int updateBook(Book b);

    /** 逻辑删除：将 available 标记为 false（仅当当前为 true 时才更新） */
    @Update("UPDATE book SET available = FALSE WHERE book_id = #{id} AND available = TRUE")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE book SET available = TRUE WHERE book_id = #{id}")
    int restoreById(@Param("id") Long id);

    /* NEW: Increment/Decrement held_quantity and recompute available */
    @Update("""
        UPDATE book 
        SET held_quantity = held_quantity + 1,
            available = (quantity - (held_quantity + 1) > 0)
        WHERE book_id = #{id}
        """)
    int incrementHeldQuantity(@Param("id") Long id);

    @Update("""
        UPDATE book 
        SET held_quantity = held_quantity - 1,
            available = (quantity - (held_quantity - 1) > 0)
        WHERE book_id = #{id} AND held_quantity > 0
        """)
    int decrementHeldQuantity(@Param("id") Long id);

    /* ================== 分页 / 搜索 ================== */

    /** 统计：把 description 纳入关键词匹配 */
    @Select("""
        <script>
        SELECT COUNT(1)
        FROM book
        <where>
          <if test="kw != null and kw != ''">
            (title LIKE CONCAT('%', #{kw}, '%')
             OR author LIKE CONCAT('%', #{kw}, '%')
             OR genre  LIKE CONCAT('%', #{kw}, '%')
             OR description LIKE CONCAT('%', #{kw}, '%'))
          </if>
          <if test="available != null">
            AND available = #{available}
          </if>
        </where>
        </script>
        """)
    long countByKeyword(@Param("kw") String keyword,
                        @Param("available") Boolean available);

    /**
     *
     *
     */
    @Select("""
        <script>
        SELECT book_id AS bookId, title, author, genre, description, quantity, held_quantity, available, path
        FROM book
        <where>
          <if test="kw != null and kw != ''">
            (title LIKE CONCAT('%', #{kw}, '%')
             OR author LIKE CONCAT('%', #{kw}, '%')
             OR genre  LIKE CONCAT('%', #{kw}, '%')
             OR description LIKE CONCAT('%', #{kw}, '%'))
          </if>
          <if test="available != null">
            AND available = #{available}
          </if>
        </where>
        ${orderBySql}
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<Book> searchByKeywordWithOrder(@Param("kw") String keyword,
                                        @Param("available") Boolean available,
                                        @Param("offset") long offset,
                                        @Param("limit") long limit,
                                        @Param("orderBySql") String orderBySql);

    /** 按体裁查询：补充 description */
    @Select("""
        SELECT book_id AS bookId, title, author, genre, description, quantity, held_quantity, available, path
        FROM book
        WHERE genre = #{genre}
        ORDER BY title
        """)
    List<Book> selectByGenre(@Param("genre") String genre);


}