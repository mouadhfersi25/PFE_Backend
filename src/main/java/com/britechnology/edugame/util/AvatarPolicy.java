package com.britechnology.edugame.util;

import com.britechnology.edugame.entity.Role;
import com.britechnology.edugame.entity.User;

public final class AvatarPolicy {

    private AvatarPolicy() {
    }

    public static String publicAvatarUrl(User user) {
        if (user == null || user.getRole() != Role.JOUEUR) {
            return null;
        }
        return user.getAvatarUrl();
    }

    public static void applyAvatarUpdate(User user, String requestedAvatarUrl) {
        if (user == null) {
            return;
        }
        if (user.getRole() != Role.JOUEUR) {
            user.setAvatarUrl(null);
            return;
        }
        if (requestedAvatarUrl != null) {
            user.setAvatarUrl(requestedAvatarUrl);
        }
    }
}
