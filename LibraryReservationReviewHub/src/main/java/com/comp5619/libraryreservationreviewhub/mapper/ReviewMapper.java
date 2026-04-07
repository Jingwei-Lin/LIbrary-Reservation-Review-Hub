package com.comp5619.libraryreservationreviewhub.mapper;

import com.comp5619.libraryreservationreviewhub.model.entity.Review;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReviewMapper {

    /**
     * Select all reviews for a specific book, ordered by review date (newest first)
     */
    @Select("SELECT review_id AS reviewId, user_id AS userId, book_id AS bookId, rating, comment, review_date AS reviewDate " +
            "FROM Review WHERE book_id = #{bookId} ORDER BY review_date DESC")
    List<Review> selectByBookId(@Param("bookId") Long bookId);

    /* ================= CRUD ================= */

    /**
     * Insert a new review
     */
    @Insert("INSERT INTO Review (user_id, book_id, rating, comment, review_date) " +
            "VALUES (#{userId}, #{bookId}, #{rating}, #{comment}, #{reviewDate})")
    @Options(useGeneratedKeys = true, keyProperty = "reviewId", keyColumn = "review_id")
    int insertReview(Review review);

    /**
     * Select a review by its ID
     */
    @Select("SELECT review_id AS reviewId, user_id AS userId, book_id AS bookId, rating, comment, review_date AS reviewDate " +
            "FROM Review WHERE review_id = #{reviewId}")
    Review selectById(@Param("reviewId") Long reviewId);

    /**
     * 根据用户ID获取该用户的所有评论（按时间倒序）
     */
    @Select("SELECT review_id AS reviewId, user_id AS userId, book_id AS bookId, rating, comment, review_date AS reviewDate " +
            "FROM Review WHERE user_id = #{userId} ORDER BY review_date DESC")
    List<Review> selectByUserId(@Param("userId") Long userId);

    /**
     * Update an existing review's rating and comment
     */
    @Update("UPDATE Review SET rating = #{rating}, comment = #{comment}, review_date = #{reviewDate} " +
            "WHERE review_id = #{reviewId}")
    int updateReview(Review review);

    /**
     * Delete a review by its ID
     */
    @Delete("DELETE FROM Review WHERE review_id = #{reviewId}")
    int deleteById(@Param("reviewId") Long reviewId);

    /**
     * Count reviews for a given book
     */
    @Select("SELECT COUNT(1) FROM Review WHERE book_id = #{bookId}")
    long countByBookId(@Param("bookId") Long bookId);

    /* ================= Pagination for site-wide reviews ================= */

    /**
     * Count reviews by optional filters (site-wide)
     */
    @Select({
        "<script>",
        "SELECT COUNT(1) FROM Review",
        "<where>",
        "  <if test=\"bookId != null\">AND book_id = #{bookId}</if>",
        "  <if test=\"userId != null\">AND user_id = #{userId}</if>",
        "  <if test=\"minRating != null\">AND rating &gt;= #{minRating}</if>",
        "  <if test=\"maxRating != null\">AND rating &lt;= #{maxRating}</if>",
        "</where>",
        "</script>"
    })
    long countAllByQuery(@Param("bookId") Long bookId,
                         @Param("userId") Long userId,
                         @Param("minRating") Integer minRating,
                         @Param("maxRating") Integer maxRating);

    /**
     * Select reviews by optional filters with pagination (site-wide)
     */
    @Select({
        "<script>",
        "SELECT review_id AS reviewId, user_id AS userId, book_id AS bookId, rating, comment, review_date AS reviewDate",
        "FROM Review",
        "<where>",
        "  <if test=\"bookId != null\">AND book_id = #{bookId}</if>",
        "  <if test=\"userId != null\">AND user_id = #{userId}</if>",
        "  <if test=\"minRating != null\">AND rating &gt;= #{minRating}</if>",
        "  <if test=\"maxRating != null\">AND rating &lt;= #{maxRating}</if>",
        "</where>",
        "ORDER BY review_date DESC",
        "LIMIT #{limit} OFFSET #{offset}",
        "</script>"
    })
    List<Review> selectAllByQuery(@Param("bookId") Long bookId,
                                  @Param("userId") Long userId,
                                  @Param("minRating") Integer minRating,
                                  @Param("maxRating") Integer maxRating,
                                  @Param("offset") long offset,
                                  @Param("limit") long limit);
}