package com.britechnology.edugame.service.player;

import com.britechnology.edugame.dto.player.RealtimeRoomPlayerDTO;
import com.britechnology.edugame.dto.player.RealtimeRoomStateDTO;
import com.britechnology.edugame.dto.player.CompetitiveRoomPlayerResultDTO;
import com.britechnology.edugame.dto.player.CompetitiveRoomResultDTO;
import com.britechnology.edugame.entity.EtatJeu;
import com.britechnology.edugame.entity.EtatSession;
import com.britechnology.edugame.entity.Jeu;
import com.britechnology.edugame.entity.ModeJeu;
import com.britechnology.edugame.entity.SessionJeu;
import com.britechnology.edugame.entity.User;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.game.SessionJeuRepository;
import com.britechnology.edugame.repository.game.JeuRepository;
import com.britechnology.edugame.repository.user.UserRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RealtimeRoomService {
    public static final int MAX_PLAYERS = 4;
    public static final int MIN_ONLINE_PLAYERS = 2;
    private static final long ROOM_TTL_MS = 2L * 60L * 60L * 1000L;
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final UserRepository userRepository;
    private final JeuRepository jeuRepository;
    private final SessionJeuRepository sessionJeuRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();

    public synchronized RealtimeRoomStateDTO createRoom(String userEmail, Long gameId) {
        if (gameId == null || gameId <= 0) {
            throw ApiException.badRequest("gameId invalide");
        }
        Jeu game = jeuRepository.findById(gameId)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        if (game.getModeJeu() != ModeJeu.EN_LIGNE) {
            throw ApiException.badRequest("Ce jeu n'est pas disponible en mode en ligne");
        }
        if (game.getEtat() != EtatJeu.ACCEPTE || !game.isActif()) {
            throw ApiException.badRequest("Ce jeu n'est pas disponible");
        }
        User user = findUser(userEmail);
        String code = generateUniqueCode();
        long now = Instant.now().toEpochMilli();

        RoomState room = new RoomState();
        room.roomCode = code;
        room.gameId = gameId;
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
        if (room.startedAt != null) {
            throw ApiException.badRequest("ROOM_ALREADY_STARTED");
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
        if (room.startedAt != null) {
            throw ApiException.badRequest("ROOM_ALREADY_STARTED");
        }
        player.ready = ready;
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
        if (room.startedAt != null) {
            throw ApiException.badRequest("ROOM_ALREADY_STARTED");
        }
        if (room.players.size() < MIN_ONLINE_PLAYERS) {
            throw ApiException.badRequest("MIN_ONLINE_PLAYERS_REQUIRED");
        }
        if (room.players.values().stream().anyMatch(p -> !p.ready)) {
            throw ApiException.badRequest("NOT_ALL_READY");
        }
        room.startedAt = Instant.now().toEpochMilli();
        RealtimeRoomStateDTO dto = toDto(room);
        broadcast(dto);
        return dto;
    }

    public synchronized OnlineRoomContext validateOnlineSubmission(
            String userEmail,
            Long gameId,
            String roomCodeRaw
    ) {
        User user = findUser(userEmail);
        RoomState room = requireRoom(normalizeRoomCode(roomCodeRaw));
        if (room.startedAt == null) {
            throw ApiException.badRequest("ROOM_NOT_STARTED");
        }
        if (!room.gameId.equals(gameId)) {
            throw ApiException.badRequest("ROOM_GAME_MISMATCH");
        }
        if (!room.players.containsKey(user.getId())) {
            throw ApiException.unauthorized("ROOM_MEMBER_REQUIRED");
        }
        return new OnlineRoomContext(
                room.roomCode,
                room.players.size()
        );
    }

    public synchronized CompetitiveRoomResultDTO getCompetitiveResult(
            String userEmail,
            String roomCodeRaw,
            Long gameId
    ) {
        User user = findUser(userEmail);
        RoomState room = requireRoom(normalizeRoomCode(roomCodeRaw));
        if (!room.players.containsKey(user.getId())) {
            throw ApiException.unauthorized("ROOM_MEMBER_REQUIRED");
        }
        if (!room.gameId.equals(gameId)) {
            throw ApiException.badRequest("ROOM_GAME_MISMATCH");
        }
        return buildCompetitiveResult(room);
    }

    public synchronized CompetitiveRoomResultDTO broadcastCompetitiveResult(String roomCodeRaw) {
        RoomState room = requireRoom(normalizeRoomCode(roomCodeRaw));
        CompetitiveRoomResultDTO result = buildCompetitiveResult(room);
        messagingTemplate.convertAndSend("/topic/rooms/" + room.roomCode + "/results", result);
        return result;
    }

    private void broadcast(RealtimeRoomStateDTO roomState) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomState.getRoomCode(), roomState);
    }

    private CompetitiveRoomResultDTO buildCompetitiveResult(RoomState room) {
        List<SessionJeu> sessions = sessionJeuRepository
                .findByJeuIdAndRoomCodeAndModeJeuLanceAndEtatSessionOrderByDateFinAsc(
                        room.gameId,
                        room.roomCode,
                        "EN_LIGNE",
                        EtatSession.TERMINE
                );

        Map<Long, SessionJeu> sessionByPlayer = sessions.stream()
                .collect(Collectors.toMap(
                        session -> session.getUtilisateur().getId(),
                        Function.identity(),
                        (first, latest) -> latest,
                        LinkedHashMap::new
                ));

        Map<Long, Integer> scores = new LinkedHashMap<>();
        room.players.values().forEach(player -> {
            SessionJeu session = sessionByPlayer.get(player.id);
            if (session != null) {
                Integer persistedScore = session.getScoreFinal() != null
                        ? session.getScoreFinal()
                        : session.getScoreGlobal();
                scores.put(
                        player.id,
                        persistedScore != null ? persistedScore : 0
                );
            }
        });
        int completedPlayers = scores.size();
        boolean complete = completedPlayers == room.players.size();
        Integer highestScore = complete
                ? scores.values().stream().max(Integer::compareTo).orElse(0)
                : null;
        long winnersCount = highestScore == null
                ? 0
                : scores.values().stream().filter(score -> score.equals(highestScore)).count();

        List<CompetitiveRoomPlayerResultDTO> players = room.players.values().stream()
                .map(player -> {
                    Integer score = scores.get(player.id);
                    String outcome = "PENDING";
                    if (complete && score != null) {
                        if (score.equals(highestScore)) {
                            outcome = winnersCount > 1 ? "DRAW" : "WINNER";
                        } else {
                            outcome = "LOSER";
                        }
                    }
                    return CompetitiveRoomPlayerResultDTO.builder()
                            .playerId(player.id)
                            .playerName(player.name)
                            .submitted(score != null)
                            .score(score)
                            .outcome(outcome)
                            .build();
                })
                .toList();

        return CompetitiveRoomResultDTO.builder()
                .roomCode(room.roomCode)
                .gameId(room.gameId)
                .expectedPlayers(room.players.size())
                .completedPlayers(completedPlayers)
                .highestScore(highestScore)
                .complete(complete)
                .players(players)
                .build();
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

    private static final class RoomState {
        private String roomCode;
        private Long gameId;
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

    public record OnlineRoomContext(
            String roomCode,
            int expectedPlayers
    ) {
    }
}
