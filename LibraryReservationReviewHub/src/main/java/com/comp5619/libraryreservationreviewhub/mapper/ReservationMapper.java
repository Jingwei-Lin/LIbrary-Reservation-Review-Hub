package com.comp5619.libraryreservationreviewhub.mapper;

import com.comp5619.libraryreservationreviewhub.model.entity.Reservation;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

@Mapper
public interface ReservationMapper {

    @Insert("""
        INSERT INTO Reservation (user_id, book_id, reservation_date, pickup_date, due_date, status, create_time)
        VALUES (#{userId}, #{bookId}, #{reservationTime}, #{pickupTime}, #{dueTime}, #{reservationStatus}, #{createTime})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "reservationId", keyColumn = "reservation_id")
    int insertReservation(Reservation reservation);

    @Select("""
        SELECT reservation_id, user_id, book_id, reservation_date AS reservationTime, 
               pickup_date AS pickupTime, due_date AS dueTime, return_date AS returnTime, 
               status AS reservationStatus, create_time, update_time, is_delete
        FROM Reservation 
        WHERE reservation_id = #{id} AND is_delete = FALSE
        """)
    Reservation selectById(@Param("id") Long id);

    @Update("""
        UPDATE Reservation 
        SET status = #{reservationStatus}, update_time = CURRENT_TIMESTAMP
        WHERE reservation_id = #{reservationId} AND is_delete = FALSE
        """)
    int updateStatus(@Param("reservationId") Long reservationId, @Param("reservationStatus") String status);

    @Update("""
        UPDATE Reservation 
        SET status = 'cancelled', update_time = CURRENT_TIMESTAMP 
        WHERE reservation_id = #{id} AND is_delete = FALSE
        """)
    int cancelById(@Param("id") Long id);

    @Select("""
        SELECT reservation_id, user_id, book_id, reservation_date AS reservationTime, 
               pickup_date AS pickupTime, due_date AS dueTime, return_date AS returnTime, 
               status AS reservationStatus, create_time, update_time, is_delete
        FROM Reservation 
        WHERE user_id = #{userId} AND is_delete = FALSE 
        ORDER BY reservation_date DESC
        """)
    List<Reservation> selectByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT reservation_id, user_id, book_id, reservation_date AS reservationTime, 
               pickup_date AS pickupTime, due_date AS dueTime, return_date AS returnTime, 
               status AS reservationStatus, create_time, update_time, is_delete
        FROM Reservation 
        WHERE is_delete = FALSE 
        ORDER BY reservation_date DESC
        """)
    List<Reservation> selectAll();

    @Select("""
        SELECT reservation_id, user_id, book_id, reservation_date AS reservationTime, 
               pickup_date AS pickupTime, due_date AS dueTime, return_date AS returnTime, 
               status AS reservationStatus, create_time, update_time, is_delete
        FROM Reservation 
        WHERE book_id = #{bookId} AND status IN ('reserved', 'borrowed') AND is_delete = FALSE
        """)
    List<Reservation> selectActiveByBookId(@Param("bookId") Long bookId);

    @Update("""
        UPDATE Reservation 
        SET pickup_date = #{pickupTime}, update_time = CURRENT_TIMESTAMP
        WHERE reservation_id = #{reservationId} AND is_delete = FALSE
        """)
    int updatePickupTime(@Param("reservationId") Long reservationId, @Param("pickupTime") Date pickupTime);

}