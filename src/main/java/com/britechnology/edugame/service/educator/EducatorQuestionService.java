package com.britechnology.edugame.service.educator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.britechnology.edugame.dto.educator.CreateQuizQuestionRequest;
import com.britechnology.edugame.dto.educator.QuizQuestionDTO;
import com.britechnology.edugame.dto.educator.UpdateQuizQuestionRequest;
import com.britechnology.edugame.entity.Jeu;
import com.britechnology.edugame.entity.Question;
import com.britechnology.edugame.entity.QuizVariant;
import com.britechnology.edugame.entity.TypeJeu;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.game.JeuRepository;
import com.britechnology.edugame.repository.game.QuestionRepository;
import com.britechnology.edugame.service.quiz.QuizVariantSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EducatorQuestionService {

    private final QuestionRepository questionRepository;
    private final JeuRepository jeuRepository;
    private final ObjectMapper objectMapper;

    public List<QuizQuestionDTO> listByGame(Long jeuId) {
        if (jeuId == null) {
            throw ApiException.badRequest("jeuId est requis");
        }
        Jeu jeu = jeuRepository.findById(jeuId)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        if (jeu.getTypeJeu() != TypeJeu.QUIZ) {
            throw ApiException.badRequest("Les questions ne sont disponibles que pour les jeux de type QUIZ");
        }
        return questionRepository.findByJeuId(jeuId).stream()
                .map(q -> toDTO(q, jeu))
                .collect(Collectors.toList());
    }

    public QuizQuestionDTO findById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Question introuvable"));
        if (question.getJeu() == null || question.getJeu().getTypeJeu() != TypeJeu.QUIZ) {
            throw ApiException.badRequest("Cette question n'est pas liée à un jeu de type QUIZ");
        }
        return toDTO(question, question.getJeu());
    }

    @Transactional
    public QuizQuestionDTO create(CreateQuizQuestionRequest request) {
        if (request == null || request.getJeuId() == null) {
            throw ApiException.badRequest("jeuId est requis");
        }
        Jeu jeu = jeuRepository.findById(request.getJeuId())
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        if (jeu.getTypeJeu() != TypeJeu.QUIZ) {
            throw ApiException.badRequest("Impossible d'ajouter une question sur un jeu qui n'est pas de type QUIZ");
        }
        EducatorGameEditPolicy.requireDraft(jeu);
        String sousType = resolveGameQuestionSousType(jeu);
        ResolvedQuestionFields resolved = resolveQuestionFields(
                sousType,
                request.getContenu(),
                request.getBonneReponse(),
                request.getOptions()
        );
        String mediaUrl = trimToNull(request.getMediaUrl());
        String promptAudioUrl = trimToNull(request.getPromptAudioUrl());
        QuizQuestionContentValidator.validateVariantMedia(sousType, mediaUrl, promptAudioUrl);
        Question question = Question.builder()
                .jeu(jeu)
                .contenu(resolved.contenu())
                .bonneReponse(resolved.bonneReponse())
                .options(optionsToJson(resolved.options()))
                .sousType(sousType)
                .mediaUrl(mediaUrl)
                .promptAudioUrl(promptAudioUrl)
                .explication(request.getExplication())
                .difficulte(request.getDifficulte())
                .build();
        question = questionRepository.save(question);
        touchGameContent(jeu);
        return toDTO(question, jeu);
    }

    @Transactional
    public QuizQuestionDTO update(Long id, UpdateQuizQuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Question introuvable"));
        if (question.getJeu() == null || question.getJeu().getTypeJeu() != TypeJeu.QUIZ) {
            throw ApiException.badRequest("Cette question n'est pas liée à un jeu de type QUIZ");
        }
        EducatorGameEditPolicy.requireDraft(question.getJeu());
        Jeu jeu = question.getJeu();
        String contenu = request.getContenu() != null ? request.getContenu() : question.getContenu();
        String sousType = resolveGameQuestionSousType(jeu);
        String bonneReponse = request.getBonneReponse() != null ? request.getBonneReponse() : question.getBonneReponse();
        List<String> options = request.getOptions() != null ? request.getOptions() : jsonToOptions(question.getOptions());
        ResolvedQuestionFields resolved = resolveQuestionFields(sousType, contenu, bonneReponse, options);
        question.setContenu(resolved.contenu());
        question.setBonneReponse(resolved.bonneReponse());
        question.setOptions(optionsToJson(resolved.options()));
        question.setSousType(sousType);
        if (request.getExplication() != null) {
            question.setExplication(request.getExplication());
        }
        if (request.getDifficulte() != null) {
            question.setDifficulte(request.getDifficulte());
        }
        String mediaUrl = request.getMediaUrl() != null ? trimToNull(request.getMediaUrl()) : question.getMediaUrl();
        String promptAudioUrl = request.getPromptAudioUrl() != null
                ? trimToNull(request.getPromptAudioUrl())
                : question.getPromptAudioUrl();
        QuizQuestionContentValidator.validateVariantMedia(sousType, mediaUrl, promptAudioUrl);
        question.setMediaUrl(mediaUrl);
        question.setPromptAudioUrl(promptAudioUrl);
        question = questionRepository.save(question);
        touchGameContent(question.getJeu());
        return toDTO(question, question.getJeu());
    }

    @Transactional
    public void delete(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Question introuvable"));
        if (question.getJeu() == null || question.getJeu().getTypeJeu() != TypeJeu.QUIZ) {
            throw ApiException.badRequest("Cette question n'est pas liée à un jeu de type QUIZ");
        }
        EducatorGameEditPolicy.requireDraft(question.getJeu());
        touchGameContent(question.getJeu());
        questionRepository.delete(question);
    }

    @Transactional
    public QuizQuestionDTO uploadMedia(Long id, MultipartFile file) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Question introuvable"));
        if (question.getJeu() == null) {
            throw ApiException.badRequest("Question sans jeu associé");
        }
        EducatorGameEditPolicy.requireDraft(question.getJeu());
        question.setMediaUrl(multipartToDataUrl(file, true));
        question = questionRepository.save(question);
        touchGameContent(question.getJeu());
        return toDTO(question, question.getJeu());
    }

    @Transactional
    public QuizQuestionDTO uploadPromptAudio(Long id, MultipartFile file) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Question introuvable"));
        if (question.getJeu() == null) {
            throw ApiException.badRequest("Question sans jeu associé");
        }
        EducatorGameEditPolicy.requireDraft(question.getJeu());
        question.setPromptAudioUrl(multipartToDataUrl(file, false));
        question = questionRepository.save(question);
        touchGameContent(question.getJeu());
        return toDTO(question, question.getJeu());
    }

    private String multipartToDataUrl(MultipartFile file, boolean image) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Fichier requis");
        }
        String contentType = file.getContentType();
        if (image) {
            if (contentType == null || !contentType.startsWith("image/")) {
                throw ApiException.badRequest("Le fichier doit être une image");
            }
        } else if (contentType == null || !contentType.startsWith("audio/")) {
            throw ApiException.badRequest("Le fichier doit être un audio");
        }
        try {
            byte[] bytes = file.getBytes();
            int maxBytes = 5 * 1024 * 1024;
            if (bytes.length > maxBytes) {
                throw ApiException.badRequest("Fichier trop volumineux (max 5 Mo)");
            }
            String mime = contentType != null ? contentType : (image ? "image/jpeg" : "audio/mpeg");
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.internalServerError("Impossible de traiter le fichier uploadé");
        }
    }

    private void touchGameContent(Jeu jeu) {
        jeu.setLastContentUpdateAt(LocalDateTime.now());
        jeuRepository.save(jeu);
    }

    private QuizQuestionDTO toDTO(Question question, Jeu jeu) {
        return QuizQuestionDTO.builder()
                .id(question.getId())
                .jeuId(jeu.getId())
                .jeuTitre(jeu.getTitre())
                .contenu(question.getContenu())
                .bonneReponse(question.getBonneReponse())
                .options(jsonToOptions(question.getOptions()))
                .sousType(QuizVariantSupport.resolveCode(jeu.getQuizVariant(), question))
                .mediaUrl(question.getMediaUrl())
                .promptAudioUrl(question.getPromptAudioUrl())
                .explication(question.getExplication())
                .difficulte(question.getDifficulte())
                .build();
    }

    private String optionsToJson(List<String> options) {
        if (options == null || options.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            throw ApiException.badRequest("Options invalides");
        }
    }

    private List<String> jsonToOptions(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static final List<String> TRUE_FALSE_OPTIONS = List.of("Vrai", "Faux");

    private record ResolvedQuestionFields(String contenu, String bonneReponse, List<String> options) {}

    private ResolvedQuestionFields resolveQuestionFields(
            String sousType,
            String contenu,
            String bonneReponse,
            List<String> options
    ) {
        String safeContenu = contenu != null ? contenu.trim() : "";
        if (safeContenu.isEmpty()) {
            throw ApiException.badRequest("Le contenu de la question est requis");
        }
        if ("TRUE_FALSE".equals(sousType)) {
            String normalizedAnswer = normalizeTrueFalseAnswer(bonneReponse);
            return new ResolvedQuestionFields(safeContenu, normalizedAnswer, TRUE_FALSE_OPTIONS);
        }
        if ("CLOZE".equals(sousType)) {
            return resolveClozeFields(safeContenu, bonneReponse, options);
        }
        if (bonneReponse == null || bonneReponse.isBlank()) {
            throw ApiException.badRequest("La bonne réponse est requise");
        }
        if (options == null || options.isEmpty()) {
            throw ApiException.badRequest("Les options de réponse sont requises");
        }
        List<String> cleaned = options.stream()
                .map(o -> o == null ? "" : o.trim())
                .filter(s -> !s.isEmpty())
                .toList();
        if (cleaned.size() < 2) {
            throw ApiException.badRequest("Au moins 2 options de réponse sont requises");
        }
        boolean answerInOptions = cleaned.stream()
                .anyMatch(opt -> opt.equalsIgnoreCase(bonneReponse.trim()));
        if (!answerInOptions) {
            throw ApiException.badRequest("La bonne réponse doit faire partie des options");
        }
        return new ResolvedQuestionFields(safeContenu, bonneReponse.trim(), cleaned);
    }

    private ResolvedQuestionFields resolveClozeFields(String contenu, String bonneReponse, List<String> options) {
        if (!contenu.contains("___")) {
            throw ApiException.badRequest("La phrase doit contenir ___ pour indiquer le mot à compléter");
        }
        if (bonneReponse == null || bonneReponse.isBlank()) {
            throw ApiException.badRequest("Sélectionnez le mot correct pour compléter la phrase");
        }
        if (options == null || options.isEmpty()) {
            throw ApiException.badRequest("Ajoutez entre 3 et 6 mots proposés");
        }
        List<String> cleaned = options.stream()
                .map(o -> o == null ? "" : o.trim())
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        if (cleaned.size() < 3 || cleaned.size() > 6) {
            throw ApiException.badRequest("Le complément de phrase accepte entre 3 et 6 mots distincts");
        }
        String answer = bonneReponse.trim();
        boolean answerInOptions = cleaned.stream().anyMatch(opt -> opt.equalsIgnoreCase(answer));
        if (!answerInOptions) {
            throw ApiException.badRequest("Le mot correct doit faire partie des propositions");
        }
        String canonicalAnswer = cleaned.stream()
                .filter(opt -> opt.equalsIgnoreCase(answer))
                .findFirst()
                .orElse(answer);
        return new ResolvedQuestionFields(contenu, canonicalAnswer, cleaned);
    }

    private String normalizeTrueFalseAnswer(String bonneReponse) {
        if (bonneReponse == null || bonneReponse.isBlank()) {
            throw ApiException.badRequest("Indiquez si l'affirmation est Vraie ou Fausse");
        }
        String normalized = bonneReponse.trim();
        if ("vrai".equalsIgnoreCase(normalized)) {
            return "Vrai";
        }
        if ("faux".equalsIgnoreCase(normalized)) {
            return "Faux";
        }
        throw ApiException.badRequest("La bonne réponse Vrai/Faux doit être « Vrai » ou « Faux »");
    }

    private String resolveGameQuestionSousType(Jeu jeu) {
        QuizVariant variant = jeu.getQuizVariant() != null ? jeu.getQuizVariant() : QuizVariant.DEFAULT;
        return variant.name();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

