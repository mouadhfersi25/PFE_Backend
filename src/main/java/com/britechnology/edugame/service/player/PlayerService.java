package com.britechnology.edugame.service.player;

import com.britechnology.edugame.dto.player.ChangePasswordRequest;
import com.britechnology.edugame.dto.player.CreateRoomRequest;
import com.britechnology.edugame.dto.player.CreateGameSessionRequest;
import com.britechnology.edugame.dto.player.CreateGameSessionResponse;
import com.britechnology.edugame.dto.player.JoinRoomRequest;
import com.britechnology.edugame.dto.player.PlayerBadgeOverviewItemDTO;
import com.britechnology.edugame.dto.player.PlayerBadgesOverviewDTO;
import com.britechnology.edugame.dto.player.PlayerHistorySessionDTO;
import com.britechnology.edugame.dto.player.PlayerOnboardingRequest;
import com.britechnology.edugame.dto.player.PlayerProgressOverviewDTO;
import com.britechnology.edugame.dto.player.PlayerRewardOverviewItemDTO;
import com.britechnology.edugame.dto.player.PlayerRewardsOverviewDTO;
import com.britechnology.edugame.dto.player.RealtimeRoomStateDTO;
import com.britechnology.edugame.dto.player.PlayerLeaderboardRanksDTO;
import com.britechnology.edugame.dto.player.SoloLeaderboardEntryDTO;
import com.britechnology.edugame.dto.player.SoloLeaderboardResponseDTO;
import com.britechnology.edugame.dto.player.CompetitiveRoomResultDTO;
import com.britechnology.edugame.dto.player.UpdateProfileRequest;
import com.britechnology.edugame.dto.player.UserDTO;
import com.britechnology.edugame.dto.reclamation.CreateReclamationRequest;
import com.britechnology.edugame.dto.reclamation.ReclamationDTO;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.service.reclamation.ReclamationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final UserService userService;
    private final RealtimeRoomService realtimeRoomService;
    private final ReclamationService reclamationService;

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

    public ReclamationDTO createReclamation(Authentication authentication, CreateReclamationRequest request) {
        return reclamationService.create(authentication, request);
    }

    public List<SoloLeaderboardEntryDTO> getSoloLeaderboard() {
        return userService.getSoloLeaderboard();
    }

    public SoloLeaderboardResponseDTO getSoloLeaderboardScoped(Authentication authentication, String scope) {
        return userService.getSoloLeaderboardScoped(authentication, scope);
    }

    public PlayerLeaderboardRanksDTO getMyLeaderboardRanks(Authentication authentication) {
        return userService.getMyLeaderboardRanks(authentication);
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
            throw ApiException.badRequest("gameId est requis");
        }
        return realtimeRoomService.createRoom(authentication.getName(), request.getGameId());
    }

    public RealtimeRoomStateDTO joinRealtimeRoom(Authentication authentication, JoinRoomRequest request) {
        if (request == null || request.getRoomCode() == null || request.getRoomCode().isBlank()) {
            throw ApiException.badRequest("roomCode est requis");
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

    public CompetitiveRoomResultDTO getCompetitiveRoomResult(
            Authentication authentication,
            String roomCode,
            Long gameId
    ) {
        return realtimeRoomService.getCompetitiveResult(authentication.getName(), roomCode, gameId);
    }
}
