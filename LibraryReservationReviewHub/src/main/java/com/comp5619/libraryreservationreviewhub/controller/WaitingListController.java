package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.annotation.AuthCheck;
import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.common.ResultUtils;
import com.comp5619.libraryreservationreviewhub.constant.UserConstant;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.*;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.mapper.BookMapper;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.entity.WaitingList;
import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import com.comp5619.libraryreservationreviewhub.model.vo.NotifyEligibilityVO;
import com.comp5619.libraryreservationreviewhub.model.vo.WaitingListVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import com.comp5619.libraryreservationreviewhub.service.WaitingListService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/waiting-list")
@RequiredArgsConstructor
public class WaitingListController {

    private final WaitingListService waitingListService;
    private final UserService userService;
    private final BookService bookService;
    private static final Logger log = LoggerFactory.getLogger(WaitingListController.class);
    private final BookMapper bookMapper;


    /* ============ 当前用户：查询 ============ */

    /**
     * 查询当前登录用户的所有 waitinglist
     * 普通用户不返回 CANCELLED；管理员则全部返回
     */
    @GetMapping("/my")
    @AuthCheck
    public BaseResponse<List<WaitingListVO>> listMyWaitlist(HttpServletRequest request) {
        User current = userService.getLoginUser(request);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_LOGIN_ERROR);

        WaitingListQueryDTO query = new WaitingListQueryDTO();
        query.setUserId(current.getId());

        List<WaitingListVO> list = waitingListService.listByUserWithDetails(current.getId());

        // 普通用户过滤 CANCELLED
        if (!userService.isAdmin(current)) {
            list.removeIf(vo -> vo.getStatus() == WaitingStatusEnum.CANCELLED);
            list.removeIf(vo -> vo.getStatus() == WaitingStatusEnum.FULFILLED);
        }
        return ResultUtils.success(list);
    }

    /**
     * 按状态查询当前登录用户的 waitinglist
     * 普通用户不可查询 CANCELLED
     * 示例：GET /waiting-list/my/status?status=waiting
     */
    @GetMapping("/my/status")
    @AuthCheck
    public BaseResponse<List<WaitingListVO>> listMyByStatus(@RequestParam("status") String status,
                                                            HttpServletRequest request) {
        User current = userService.getLoginUser(request);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_LOGIN_ERROR);

        WaitingStatusEnum s;
        try {
            s = WaitingStatusEnum.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "Illegal state parameter: " + status);
            return ResultUtils.success(List.of()); // unreachable，仅为语法完整
        }

        // 普通用户禁止查取消状态
        ThrowUtils.throwIf(!userService.isAdmin(current) && s == WaitingStatusEnum.CANCELLED,
                ErrorCode.FORBIDDEN_ERROR, "Ordinary users are not allowed to view the cancelled records.");

        WaitingListQueryDTO query = new WaitingListQueryDTO();
        query.setUserId(current.getId());
        query.setStatus(s);

        return ResultUtils.success(waitingListService.listByQuery(query));
    }

    /* ============ 当前用户：添加/取消 ============ */

    /**
     * 当前用户添加一本书到自己的 waitinglist
     * 限制：同一用户对同一本书，存在“有效(非 CANCELLED)”记录则不允许重复添加
     */
    @PostMapping("/my/add")
    @AuthCheck
    public BaseResponse<Long> addToMyWaitlist(@Valid @RequestBody AddReq req, HttpServletRequest request) {
        User current = userService.getLoginUser(request);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_LOGIN_ERROR);

        // 是否已存在有效记录
        long exists = waitingListService.lambdaQuery()
                .eq(WaitingList::getUserId, current.getId())
                .eq(WaitingList::getBookId, req.getBookId())
                .ne(WaitingList::getStatus, WaitingStatusEnum.CANCELLED)
                .ne(WaitingList::getStatus, WaitingStatusEnum.FULFILLED)
                .count();
        ThrowUtils.throwIf(exists > 0, ErrorCode.PARAMS_ERROR, "This book is currently on the waiting list.");

        WaitingListCreateDTO dto = new WaitingListCreateDTO();
        dto.setUserId(current.getId());
        dto.setBookId(req.getBookId());

        Long id = waitingListService.create(dto);
        ThrowUtils.throwIf(id == null, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(id);
    }

    /**
     * 当前用户取消自己对某本书的 waiting（设为 CANCELLED）
     * 示例：DELETE /waiting-list/my/123
     */
    @DeleteMapping("/my/{bookId}")
    @AuthCheck
    public BaseResponse<Boolean> cancelMyWait(@PathVariable("bookId") Long bookId, HttpServletRequest request) {
        User current = userService.getLoginUser(request);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_LOGIN_ERROR);

        // 找到“有效”记录
        WaitingList record = waitingListService.lambdaQuery()
                .eq(WaitingList::getUserId, current.getId())
                .eq(WaitingList::getBookId, bookId)
                .in(WaitingList::getStatus, WaitingStatusEnum.WAITING, WaitingStatusEnum.NOTIFIED)
                .one();
        ThrowUtils.throwIf(record == null, ErrorCode.NOT_FOUND_ERROR, "No cancelable waiting record was found.");

        Long waitId = record.getWaitId();
        WaitingStatusEnum oldStatus = record.getStatus();

        // Mark as CANCELLED
        WaitingListUpdateDTO dto = new WaitingListUpdateDTO();
        dto.setWaitId(record.getWaitId());
        dto.setStatus(WaitingStatusEnum.CANCELLED);
        dto.setIsNotified(false);
        boolean ok = waitingListService.updateStatus(dto);
        ThrowUtils.throwIf(!ok, ErrorCode.OPERATION_ERROR, "Cancellation failed");

        // Only if it was NOTIFIED → release the held copy
        if (WaitingStatusEnum.NOTIFIED.equals(oldStatus)) {
            Book book = bookService.getBookById(bookId);
            ThrowUtils.throwIf(book == null, ErrorCode.NOT_FOUND_ERROR);

            // Find next waiting user
            WaitingList nextWaiting = waitingListService.lambdaQuery()
                    .eq(WaitingList::getBookId, bookId)
                    .eq(WaitingList::getStatus, WaitingStatusEnum.WAITING)
                    .orderByAsc(WaitingList::getJoinDate)
                    .last("LIMIT 1")
                    .one();

            if (nextWaiting != null) {
                // Notify next user
                // waitingListService.computeNotifyEligibilityForUser(current.getId());
                log.info("Cancelled NOTIFIED entry; next user in line → held_quantity incremented for book {}", bookId);
            } else {
                // No one waiting → release held copy back to public quantity
                book.setHeldQuantity(Math.max(0, book.getHeldQuantity() - 1));
                book.setQuantity(book.getQuantity() + 1);
                book.setAvailable(true);

                int rows = bookMapper.updateBook(book);
                bookService.decrementHeldQuantity(bookId);
                ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR, "Failed to release held copy");

                log.info("Cancelled NOTIFIED entry; no next user → held_quantity--, quantity++ for book {}", bookId);
            }
        }

        return ResultUtils.success(true);
    }

    /* ============ 管理员：按用户ID查看（可选） ============ */

    /**
     * 管理员按用户ID查看其所有 waitinglist（包含 CANCELLED）
     */
    @GetMapping("/user/{userId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<WaitingListVO>> listByUserAdmin(@PathVariable("userId") Long userId) {
        WaitingListQueryDTO query = new WaitingListQueryDTO();
        query.setUserId(userId);
        return ResultUtils.success(waitingListService.listByQuery(query));
    }

    /* ============ 分页（如需与 PageRequest 对齐，可扩展） ============ */

    /**
     * 示例：GET /waiting-list/my/page?current=1&pageSize=10
     */
    @GetMapping("/my/page")
    @AuthCheck
    public BaseResponse<PageResult<WaitingListVO>> pageMyWaitlist(PageRequest pageReq,
                                                                  HttpServletRequest request) {
        User current = userService.getLoginUser(request);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_LOGIN_ERROR);

        // 仅查当前用户
        WaitingListQueryDTO query = new WaitingListQueryDTO();
        query.setUserId(current.getId());
        PageResult<WaitingListVO> page = waitingListService.pageByQuery(query, pageReq);

        // 普通用户不显示 CANCELLED（仅过滤当前页记录）
        if (!userService.isAdmin(current) && page.getRecords() != null) {
            var filtered = page.getRecords().stream()
                    .filter(vo -> vo.getStatus() != WaitingStatusEnum.CANCELLED)
                    .toList();
            page.setRecords(filtered);
        }

        return ResultUtils.success(page);
    }

    /**通知该用户当前可以借阅的书籍**/
    @GetMapping("/my/eligibility")
    @AuthCheck
    public BaseResponse<List<NotifyEligibilityVO>> myNotifyEligibility(HttpServletRequest request) {
        User current = userService.getLoginUser(request);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_LOGIN_ERROR);

        // 仅基于 WAITING 的记录计算
        List<NotifyEligibilityVO> list = waitingListService.computeNotifyEligibilityForUser(current.getId());

        // （可选）普通用户只看自己，不做额外过滤。若你要隐藏库存字段给普通用户，这里可做脱敏。
        return ResultUtils.success(list);
    }


    @Data
    public static class AddReq {
        @NotNull(message = "bookId can not be null")
        private Long bookId;
    }
}