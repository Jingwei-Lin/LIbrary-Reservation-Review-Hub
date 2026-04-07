package com.comp5619.libraryreservationreviewhub.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.*;
import com.comp5619.libraryreservationreviewhub.model.entity.WaitingList;
import com.comp5619.libraryreservationreviewhub.model.vo.NotifyEligibilityVO;
import com.comp5619.libraryreservationreviewhub.model.vo.WaitingListVO;

import java.util.List;

/**
 * 等待列表业务接口
 */
public interface WaitingListService extends IService<WaitingList> {

    /** 创建等待记录 */
    Long create(WaitingListCreateDTO dto);

    /** 更新等待记录状态 */
    boolean updateStatus(WaitingListUpdateDTO dto);

    /** 删除等待记录 */
    boolean deleteById(Long id);

    /** 按ID查询 */
    WaitingListVO getById(Long id);

    /** 条件查询列表 */
    List<WaitingListVO> listByQuery(WaitingListQueryDTO dto);

    List<WaitingListVO> listByUserWithDetails(Long userId);

    /**分页**/
    PageResult<WaitingListVO> pageByQuery(WaitingListQueryDTO query, PageRequest pageReq);

    /**Notify users of the current books available for borrowing**/
    List<NotifyEligibilityVO> computeNotifyEligibilityForUser(Long id);

    /**Automatically update the status of the waiting list**/
    int cancelStaleWaiting();
}
