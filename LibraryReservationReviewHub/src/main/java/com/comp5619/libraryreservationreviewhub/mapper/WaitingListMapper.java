package com.comp5619.libraryreservationreviewhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import com.comp5619.libraryreservationreviewhub.model.entity.WaitingList;
import com.comp5619.libraryreservationreviewhub.model.vo.NotifyEligibilityVO;
import com.comp5619.libraryreservationreviewhub.model.vo.WaitingListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WaitingListMapper extends BaseMapper<WaitingList> {

    // 直接用内置 CRUD
    // 也可以定义自定义 SQL
    List<WaitingListVO> listByUser(@Param("userId") Long userId);

    int updateStatus(@Param("waitId") Long waitId,
                     @Param("status") WaitingStatusEnum status);

    /** 取当前用户在所有 WAITING 图书队列中的位置 + 图书库存 */
    List<NotifyEligibilityVO> selectNotifyEligibilityForUser(@Param("userId") Long userId);

    /** 将该用户在这些 bookIds 的 WAITING 记录改为 NOTIFIED，写入通知时间、is_notified */
    int batchNotifyByBooks(@Param("userId") Long userId,
                           @Param("bookIds") List<Long> bookIds);

    /** 将超过 7 天仍为 waiting 的记录置为 cancelled，返回受影响行数 */
    int cancelStaleWaiting();
}

