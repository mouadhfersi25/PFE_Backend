package com.britechnology.edugame.service.voice;

import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.badge.NiveauRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerProgressionService {

    private final NiveauRepository niveauRepository;

    public record ProgressionResult(int newLevel, int newXp, int xpToNextLevel) {}

    public ProgressionResult applyProgression(
            int currentLevel,
            int currentXp,
            int xpGained,
            boolean allowLevelUp,
            boolean maxOneLevelUpPerSession
    ) {
        int level = Math.max(1, currentLevel);
        int xp = Math.max(0, currentXp) + Math.max(0, xpGained);
        int required = xpToNextLevel(level);

        if (!allowLevelUp) {
            if (xp >= required) {
                xp = Math.max(0, required - 1);
            }
            return new ProgressionResult(level, xp, required);
        }

        boolean levelUpsApplied = false;
        while (xp >= required) {
            xp -= required;
            level += 1;
            levelUpsApplied = true;
            required = xpToNextLevel(level);
            if (maxOneLevelUpPerSession && levelUpsApplied) {
                break;
            }
        }

        return new ProgressionResult(level, xp, required);
    }

    public int xpToNextLevel(int level) {
        return niveauRepository.findByNiveau(level)
                .map(cfg -> Math.max(1, cfg.getPointMin() != null ? cfg.getPointMin() : 0))
                .orElse(Math.max(250, (level * 150) + (level * level * 55)));
    }
}
