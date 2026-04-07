package com.comp5619.libraryreservationreviewhub.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.mapper.WaitingListMapper;
import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListCreateDTO;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListQueryDTO;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListUpdateDTO;
import com.comp5619.libraryreservationreviewhub.model.entity.WaitingList;
import com.comp5619.libraryreservationreviewhub.model.vo.NotifyEligibilityVO;
import com.comp5619.libraryreservationreviewhub.model.vo.WaitingListVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.WaitingListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 等待列表业务实现类（参照 BookServiceImpl 风格）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WaitingListServiceImpl
        extends ServiceImpl<WaitingListMapper, WaitingList>
        implements WaitingListService {

    /* ---------- 分页与排序拼装（带字段白名单） ---------- */
    private record PageCalc(int current, int size, String sortField, boolean asc) {}

    /** 仅允许这些字段参与排序（列名与数据库一致） */
    private static final Set<String> SORT_WHITELIST = Set.of(
            "wait_id", "user_id", "book_id", "join_date", "notification_date", "is_notified", "status"
    );

    private final BookService bookService;

    /** 计算分页与排序参数（默认 wait_id DESC） */
    private PageCalc calc(PageRequest pr) {
        int current = Math.max(1, pr.getCurrent());
        int size = Math.min(Math.max(1, pr.getPageSize()), 101);

        String sortField = "wait_id";
        boolean asc = false; // 默认 DESC

        if (pr.getSortField() != null && SORT_WHITELIST.contains(pr.getSortField())) {
            sortField = pr.getSortField();
            asc = !"descend".equalsIgnoreCase(String.valueOf(pr.getSortOrder()));
        }
        return new PageCalc(current, size, sortField, asc);
    }

    /* ===================== CRUD ===================== */

    @Override
    public Long create(WaitingListCreateDTO dto) {
        ThrowUtils.throwIf(dto == null, ErrorCode.PARAMS_ERROR);

        WaitingList entity = new WaitingList();
        BeanUtils.copyProperties(dto, entity);

        entity.setJoinDate(LocalDateTime.now());
        entity.setStatus(WaitingStatusEnum.WAITING);
        entity.setIsNotified(Boolean.FALSE);

        int rows = baseMapper.insert(entity);
        ThrowUtils.throwIf(rows <= 0 || entity.getWaitId() == null, ErrorCode.OPERATION_ERROR);
        return entity.getWaitId();
    }

    @Override
    public boolean updateStatus(WaitingListUpdateDTO dto) {
        ThrowUtils.throwIf(dto == null || dto.getWaitId() == null, ErrorCode.PARAMS_ERROR);

        WaitingList entity = baseMapper.selectById(dto.getWaitId());
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR, "等待记录不存在");

        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getIsNotified() != null) {
            entity.setIsNotified(dto.getIsNotified());
        }
        if (dto.getNotificationDate() != null) {
            entity.setNotificationDate(dto.getNotificationDate());
        }

        int rows = baseMapper.updateById(entity);
        ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public boolean deleteById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    public WaitingListVO getById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        WaitingList entity = baseMapper.selectById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);

        WaitingListVO vo = new WaitingListVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /* ===================== 查询 ===================== */

    @Override
    public List<WaitingListVO> listByQuery(WaitingListQueryDTO dto) {
        QueryWrapper<WaitingList> wrapper = buildWrapper(dto);

        if (dto.getOrderBy() != null && !dto.getOrderBy().isBlank()) {
            String ob = dto.getOrderBy().trim().toLowerCase(Locale.ROOT);
            String field = ob.split("\\s+")[0];
            if (SORT_WHITELIST.contains(field)) {
                wrapper.last("ORDER BY " + ob);
            }
        } else {
            wrapper.orderByDesc("wait_id");
        }

        List<WaitingList> list = baseMapper.selectList(wrapper);
        if (CollUtil.isEmpty(list)) return Collections.emptyList();

        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<WaitingListVO> listByUserWithDetails(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        List<WaitingListVO> list = baseMapper.listByUser(userId);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().collect(Collectors.toList());
    }

    /**
     * 分页查询
     */
    public PageResult<WaitingListVO> pageByQuery(WaitingListQueryDTO dto, PageRequest pageReq) {
        PageCalc p = calc(pageReq);
        QueryWrapper<WaitingList> wrapper = buildWrapper(dto);

        // 排序
        if (p.asc) {
            wrapper.orderByAsc(p.sortField);
        } else {
            wrapper.orderByDesc(p.sortField);
        }

        // 统计总数
        long total = baseMapper.selectCount(wrapper);

        // 计算分页窗口
        if (total == 0) {
            return new PageResult<>(pageReq.getCurrent(), pageReq.getPageSize(), 0, List.of());
        }

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WaitingList> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(p.current, p.size, total);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WaitingList> entityPage =
                baseMapper.selectPage(page, wrapper);

        List<WaitingListVO> records = entityPage.getRecords()
                .stream().map(this::toVO).collect(Collectors.toList());

        return new PageResult<>(pageReq.getCurrent(), pageReq.getPageSize(), entityPage.getTotal(), records);
    }

    /* ===================== 私有工具 ===================== */

    private WaitingListVO toVO(WaitingList entity) {
        WaitingListVO vo = new WaitingListVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private QueryWrapper<WaitingList> buildWrapper(WaitingListQueryDTO dto) {
        QueryWrapper<WaitingList> wrapper = new QueryWrapper<>();
        if (dto == null) return wrapper;

        if (dto.getUserId() != null) {
            wrapper.eq("user_id", dto.getUserId());
        }
        if (dto.getBookId() != null) {
            wrapper.eq("book_id", dto.getBookId());
        }
        if (dto.getStatus() != null) {
            wrapper.eq("status", dto.getStatus());
        }
        if (dto.getIsNotified() != null) {
            wrapper.eq("is_notified", dto.getIsNotified());
        }
        return wrapper;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<NotifyEligibilityVO> computeNotifyEligibilityForUser(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);

        // Query current user's WAITING records with position and notified count info
        List<NotifyEligibilityVO> rows = baseMapper.selectNotifyEligibilityForUser(userId);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        // Determine which books can notify the current user
        List<Long> eligibleBookIds = new ArrayList<>();
        for (var vo : rows) {
            // Check conditions for notification:
            // 1. Book has available copies on hold (heldQuantity > 0)
            // 2. User's position is within available held copies (position <= heldQuantity)
            // 3. No other users are already notified for this book (notifiedCount == 0)
            boolean can = vo.getHeldQuantity() != null
                    && (vo.getHeldQuantity() > 0 || vo.getQuantity() > 0)
                    && vo.getPosition() != null
                    && vo.getPosition() <= vo.getHeldQuantity()
                    && vo.getNotifiedCount() != null
                    && vo.getNotifiedCount() < vo.getHeldQuantity();
            vo.setCanNotify(can);
            if (can) {
                eligibleBookIds.add(vo.getBookId());
            }
        }

        // Batch update: set these waiting records to NOTIFIED
        if (!eligibleBookIds.isEmpty()) {
            int updated = baseMapper.batchNotifyByBooks(userId, eligibleBookIds);
            ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "Failed to update the notification status");
        }

        // Log the notification
        log.info("Notified user {} for books: {}", userId, eligibleBookIds);

        return rows;
    }

    @Override
    public int cancelStaleWaiting() {
        int rows = baseMapper.cancelStaleWaiting();
        log.info("Cancelled {} stale waiting-list records (>7 days).", rows);
        return rows;
    }

}
