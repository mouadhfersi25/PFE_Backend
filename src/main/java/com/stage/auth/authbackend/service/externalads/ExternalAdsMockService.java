package com.stage.auth.authbackend.service.externalads;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ExternalAdsMockService {

    private final List<Map<String, Object>> rewards = new ArrayList<>();
    private final String configuredApiKey;

    public ExternalAdsMockService(@Value("${external.ads.api-key:}") String configuredApiKey) {
        this.configuredApiKey = configuredApiKey == null ? "" : configuredApiKey;
        seedData();
    }

    public boolean isAuthorized(String providedApiKey) {
        if (configuredApiKey.isBlank()) return true;
        return configuredApiKey.equals(providedApiKey);
    }

    public synchronized Map<String, Object> stats() {
        return Map.of(
                "activeCampaigns", 0,
                "totalImpressions", 0,
                "totalClicks", 0,
                "ctr", 0.0,
                "distributedRewards", 0,
                "rewardStock", rewards.size()
        );
    }

    public synchronized List<Map<String, Object>> listRewards() {
        return rewards;
    }

    private void seedData() {
        rewards.add(Map.of(
                "id", 1,
                "name", "Coupon 10%",
                "description", "Reduction partenaire",
                "scoreMin", 120,
                "type", "COUPON",
                "sponsorName", "Sponsor Workspace"
        ));
        rewards.add(Map.of(
                "id", 2,
                "name", "Gift Box",
                "description", "Pack cadeau educational",
                "scoreMin", 200,
                "type", "GIFT",
                "sponsorName", "Sponsor Workspace"
        ));
    }
}
