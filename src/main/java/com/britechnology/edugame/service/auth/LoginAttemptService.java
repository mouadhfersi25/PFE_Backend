package com.britechnology.edugame.service.auth;

import com.britechnology.edugame.entity.User;
import com.britechnology.edugame.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Enregistre les échecs de connexion dans leur propre transaction (REQUIRES_NEW),
 * pour que le compteur soit bien persisté même si AuthService#login (transactionnel)
 * termine en levant une exception juste après — sinon le rollack de la transaction
 * appelante annulerait aussi cet enregistrement.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailedAttempt(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        int attempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
        } else {
            user.setFailedLoginAttempts(attempts);
        }
        userRepository.save(user);
    }
}
