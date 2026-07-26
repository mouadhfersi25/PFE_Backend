package com.stage.auth.authbackend.service.player;

import com.stage.auth.authbackend.dto.player.RealtimeRoomPlayerDTO;
import com.stage.auth.authbackend.dto.player.RealtimeRoomStateDTO;
import com.stage.auth.authbackend.entity.User;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RealtimeRoomService {
    public static final int MAX_PLAYERS = 4;
    private static final long ROOM_TTL_MS = 2L * 60L * 60L * 1000L;
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();

    public synchronized RealtimeRoomStateDTO createRoom(String userEmail, Long gameId, String teamNameRaw) {
        if (gameId == null || gameId <= 0) {
            throw ApiException.badRequest("gameId invalide");
        }
        User user = findUser(userEmail);
        String code = generateUniqueCode();
        long now = Instant.now().toEpochMilli();

        RoomState room = new RoomState();
        room.roomCode = code;
        room.gameId = gameId;
        room.teamName = normalizeTeamName(teamNameRaw);
        room.createdAt = now;
        room.startedAt = null;
        room.players = new LinkedHashMap<>();
        room.players.put(user.getId(), toStatePlayer(user, true));
        rooms.put(code, room);

        RealtimeRoomStateDTO dto = toDto(room);
        broadcast(dto);
        return dto;
    }

    public synchronized RealtimeRoomStateDTO joinRoom(String userEmail, String roomCodeRaw) {
        User user = findUser(userEmail);
        String roomCode = normalizeRoomCode(roomCodeRaw);
        RoomState room = requireRoom(roomCode);

        if (room.players.containsKey(user.getId())) {
            RealtimeRoomStateDTO dto = toDto(room);
            broadcast(dto);
            return dto;
        }
        if (room.players.size() >= MAX_PLAYERS) {
            throw ApiException.badRequest("ROOM_FULL");
        }

        room.players.put(user.getId(), toStatePlayer(user, false));
        RealtimeRoomStateDTO dto = toDto(room);
        broadcast(dto);
        return dto;
    }

    public synchronized RealtimeRoomStateDTO getRoom(String roomCodeRaw) {
        String roomCode = normalizeRoomCode(roomCodeRaw);
        return toDto(requireRoom(roomCode));
    }

    public synchronized RealtimeRoomStateDTO setReady(String userEmail, String roomCodeRaw, boolean ready) {
        User user = findUser(userEmail);
        RoomState room = requireRoom(normalizeRoomCode(roomCodeRaw));
        StatePlayer player = room.players.get(user.getId());
        if (player == null) {
            throw ApiException.unauthorized("ROOM_MEMBER_REQUIRED");
        }
        player.ready = ready;
        RealtimeRoomStateDTO dto = toDto(room);
        broadcast(dto);
        return dto;
    }

    public synchronized RealtimeRoomStateDTO updateTeamName(String userEmail, String roomCodeRaw, String teamNameRaw) {
        User user = findUser(userEmail);
        RoomState room = requireRoom(normalizeRoomCode(roomCodeRaw));
        StatePlayer player = room.players.get(user.getId());
        if (player == null) {
            throw ApiException.unauthorized("ROOM_MEMBER_REQUIRED");
        }
        if (!player.host) {
            throw ApiException.unauthorized("HOST_ONLY");
        }
        String normalized = normalizeTeamName(teamNameRaw);
        if (normalized == null || normalized.length() < 2) {
            throw ApiException.badRequest("INVALID_TEAM_NAME");
        }
        room.teamName = normalized;
        RealtimeRoomStateDTO dto = toDto(room);
        broadcast(dto);
        return dto;
    }

    public synchronized RealtimeRoomStateDTO startRoom(String userEmail, String roomCodeRaw) {
        User user = findUser(userEmail);
        RoomState room = requireRoom(normalizeRoomCode(roomCodeRaw));
        StatePlayer player = room.players.get(user.getId());
        if (player == null) {
            throw ApiException.unauthorized("ROOM_MEMBER_REQUIRED");
        }
        if (!player.host) {
            throw ApiException.unauthorized("HOST_ONLY");
        }
        if (room.players.isEmpty() || room.players.values().stream().anyMatch(p -> !p.ready)) {
            throw ApiException.badRequest("NOT_ALL_READY");
        }
        room.startedAt = Instant.now().toEpochMilli();
        RealtimeRoomStateDTO dto = toDto(room);
        broadcast(dto);
        return dto;
    }

    private void broadcast(RealtimeRoomStateDTO roomState) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomState.getRoomCode(), roomState);
    }

    private RoomState requireRoom(String roomCode) {
        RoomState room = rooms.get(roomCode);
        if (room == null) {
            throw ApiException.notFound("ROOM_NOT_FOUND");
        }
        long now = Instant.now().toEpochMilli();
        if (now - room.createdAt > ROOM_TTL_MS) {
            rooms.remove(roomCode);
            throw ApiException.notFound("STALE_ROOM");
        }
        return room;
    }

    private User findUser(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw ApiException.unauthorized("UNAUTHORIZED");
        }
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
    }

    private RealtimeRoomStateDTO toDto(RoomState room) {
        List<RealtimeRoomPlayerDTO> players = new ArrayList<>(room.players.values().stream()
                .sorted(Comparator.comparing((StatePlayer p) -> !p.host).thenComparing(StatePlayer::name))
                .map(p -> RealtimeRoomPlayerDTO.builder()
                        .id(p.id)
                        .name(p.name)
                        .avatar(p.avatar)
                        .age(p.age)
                        .ready(p.ready)
                        .host(p.host)
                        .build())
                .toList());

        boolean allReady = !players.isEmpty() && players.stream().allMatch(RealtimeRoomPlayerDTO::isReady);
        return RealtimeRoomStateDTO.builder()
                .roomCode(room.roomCode)
                .gameId(room.gameId)
                .teamName(room.teamName)
                .maxPlayers(MAX_PLAYERS)
                .createdAt(room.createdAt)
                .startedAt(room.startedAt)
                .allReady(allReady)
                .players(players)
                .build();
    }

    private StatePlayer toStatePlayer(User user, boolean host) {
        String displayName = (String.format("%s %s",
                user.getPrenom() == null ? "" : user.getPrenom(),
                user.getNom() == null ? "" : user.getNom())).trim();
        if (displayName.isBlank()) displayName = user.getEmail();
        StatePlayer player = new StatePlayer();
        player.id = user.getId();
        player.name = displayName;
        player.avatar = (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) ? "👤" : user.getAvatarUrl();
        player.age = null;
        player.ready = false;
        player.host = host;
        return player;
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 20; i++) {
            String code = randomCode();
            if (!rooms.containsKey(code)) return code;
        }
        throw ApiException.badRequest("Impossible de générer un code room unique");
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            builder.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return builder.toString();
    }

    private String normalizeRoomCode(String roomCodeRaw) {
        if (roomCodeRaw == null || roomCodeRaw.isBlank()) {
            throw ApiException.badRequest("roomCode est requis");
        }
        return roomCodeRaw.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTeamName(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim();
        if (normalized.isBlank()) return null;
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }

    private static final class RoomState {
        private String roomCode;
        private Long gameId;
        private String teamName;
        private long createdAt;
        private Long startedAt;
        private Map<Long, StatePlayer> players;
    }

    private static final class StatePlayer {
        private Long id;
        private String name;
        private String avatar;
        private Integer age;
        private boolean ready;
        private boolean host;

        public String name() {
            return name == null ? "" : name;
        }
    }
}
