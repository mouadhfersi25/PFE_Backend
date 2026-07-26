package com.stage.auth.authbackend.service.player;

import com.stage.auth.authbackend.dto.player.ChangePasswordRequest;
import com.stage.auth.authbackend.dto.player.CreateRoomRequest;
import com.stage.auth.authbackend.dto.player.CreateGameSessionRequest;
import com.stage.auth.authbackend.dto.player.CreateGameSessionResponse;
import com.stage.auth.authbackend.dto.player.JoinRoomRequest;
import com.stage.auth.authbackend.dto.player.PlayerBadgeOverviewItemDTO;
import com.stage.auth.authbackend.dto.player.PlayerBadgesOverviewDTO;
import com.stage.auth.authbackend.dto.player.PlayerHistorySessionDTO;
import com.stage.auth.authbackend.dto.player.PlayerOnboardingRequest;
import com.stage.auth.authbackend.dto.player.PlayerProgressOverviewDTO;
import com.stage.auth.authbackend.dto.player.PlayerRewardOverviewItemDTO;
import com.stage.auth.authbackend.dto.player.PlayerRewardsOverviewDTO;
import com.stage.auth.authbackend.dto.player.RealtimeRoomStateDTO;
import com.stage.auth.authbackend.dto.player.SoloLeaderboardEntryDTO;
import com.stage.auth.authbackend.dto.player.TeamLeaderboardEntryDTO;
import com.stage.auth.authbackend.dto.player.UpdateProfileRequest;
import com.stage.auth.authbackend.dto.player.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final UserService userService;
    private final RealtimeRoomService realtimeRoomService;

    public UserDTO updateProfile(Authentication authentication, UpdateProfileRequest request) {
        return userService.updateProfile(authentication, request);
    }

    public void changePassword(Authentication authentication, ChangePasswordRequest request) {
        userService.changePassword(authentication, request);
    }

    public UserDTO completeOnboarding(Authentication authentication, PlayerOnboardingRequest request) {
        return userService.completeOnboarding(authentication, request);
    }

    public CreateGameSessionResponse createGameSession(Authentication authentication, CreateGameSessionRequest request) {
        return userService.createGameSession(authentication, request);
    }

    public List<SoloLeaderboardEntryDTO> getSoloLeaderboard() {
        return userService.getSoloLeaderboard();
    }

    public List<TeamLeaderboardEntryDTO> getTeamLeaderboard() {
        return userService.getTeamLeaderboard();
    }

    public PlayerProgressOverviewDTO getProgressOverview(Authentication authentication) {
        return userService.getProgressOverview(authentication);
    }

    public PlayerBadgesOverviewDTO getBadgesOverview(Authentication authentication) {
        return userService.getBadgesOverview(authentication);
    }

    public PlayerBadgeOverviewItemDTO claimBadge(Authentication authentication, Long badgeId) {
        return userService.claimBadge(authentication, badgeId);
    }

    public List<PlayerHistorySessionDTO> getHistorySessions(Authentication authentication) {
        return userService.getHistorySessions(authentication);
    }

    public PlayerRewardsOverviewDTO getRewardsOverview(Authentication authentication) {
        return userService.getRewardsOverview(authentication);
    }

    public PlayerRewardOverviewItemDTO claimReward(Authentication authentication, Long rewardId) {
        return userService.claimReward(authentication, rewardId);
    }

    public RealtimeRoomStateDTO createRealtimeRoom(Authentication authentication, CreateRoomRequest request) {
        if (request == null || request.getGameId() == null) {
            throw com.stage.auth.authbackend.exception.ApiException.badRequest("gameId est requis");
        }
        return realtimeRoomService.createRoom(authentication.getName(), request.getGameId(), request.getTeamName());
    }

    public RealtimeRoomStateDTO joinRealtimeRoom(Authentication authentication, JoinRoomRequest request) {
        if (request == null || request.getRoomCode() == null || request.getRoomCode().isBlank()) {
            throw com.stage.auth.authbackend.exception.ApiException.badRequest("roomCode est requis");
        }
        return realtimeRoomService.joinRoom(authentication.getName(), request.getRoomCode());
    }

    public RealtimeRoomStateDTO getRealtimeRoom(String roomCode) {
        return realtimeRoomService.getRoom(roomCode);
    }

    public RealtimeRoomStateDTO setRealtimeRoomReady(Authentication authentication, String roomCode, boolean ready) {
        return realtimeRoomService.setReady(authentication.getName(), roomCode, ready);
    }

    public RealtimeRoomStateDTO startRealtimeRoom(Authentication authentication, String roomCode) {
        return realtimeRoomService.startRoom(authentication.getName(), roomCode);
    }

    public RealtimeRoomStateDTO updateRealtimeRoomTeamName(Authentication authentication, String roomCode, String teamName) {
        return realtimeRoomService.updateTeamName(authentication.getName(), roomCode, teamName);
    }
}
