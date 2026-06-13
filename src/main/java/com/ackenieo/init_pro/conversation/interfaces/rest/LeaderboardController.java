package com.ackenieo.init_pro.conversation.interfaces.rest;

import com.ackenieo.init_pro.conversation.application.service.LeaderboardAppService;
import com.ackenieo.init_pro.conversation.interfaces.dto.LeaderboardItemResponse;
import com.ackenieo.init_pro.conversation.interfaces.dto.LeaderboardResponse;
import com.ackenieo.init_pro.shared.infrastructure.ApiResponse;
import com.ackenieo.init_pro.shared.infrastructure.BaseController;
import com.ackenieo.init_pro.user.infrastructure.security.CurrentUserConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 排行榜控制器
 */
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController extends BaseController {

    private final LeaderboardAppService leaderboardAppService;

    public LeaderboardController(LeaderboardAppService leaderboardAppService) {
        this.leaderboardAppService = leaderboardAppService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getLeaderboard(HttpServletRequest request,
                                                                           @RequestParam(defaultValue = "1") int page,
                                                                           @RequestParam(defaultValue = "20") int pageSize) {
        return success(leaderboardAppService.getLeaderboard(currentUserId(request), page, pageSize));
    }

    @GetMapping("/my-rank")
    public ResponseEntity<ApiResponse<LeaderboardItemResponse>> getMyRank(HttpServletRequest request) {
        return success(leaderboardAppService.getMyRank(currentUserId(request)));
    }

    private String currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(CurrentUserConstants.CURRENT_USER_ID);
        if (userId == null) {
            throw new RuntimeException("当前用户未登录");
        }
        return String.valueOf(userId);
    }
}
