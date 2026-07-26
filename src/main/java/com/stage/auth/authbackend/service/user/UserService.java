package com.stage.auth.authbackend.service.user;

import com.stage.auth.authbackend.dto.user.ChangePasswordRequest;
import com.stage.auth.authbackend.dto.user.PlayerOnboardingRequest;
import com.stage.auth.authbackend.dto.user.UpdateProfileRequest;
import com.stage.auth.authbackend.dto.user.UserDTO;
import com.stage.auth.authbackend.entity.*;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.geo.PaysRepository;
import com.stage.auth.authbackend.repository.geo.RegionRepository;
import com.stage.auth.authbackend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaysRepository paysRepository;
    private final RegionRepository regionRepository;

    public UserDTO updateProfile(Authentication authentication, UpdateProfileRequest request) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));

        // Mise à jour des champs modifiables (prénom/nom conservés tels quels, pas de trim)
        if (request.getNom() != null) {
            user.setNom(request.getNom());
        }
        if (request.getPrenom() != null) {
            user.setPrenom(request.getPrenom());
        }
        if (request.getTelephone() != null) {
            user.setTelephone(request.getTelephone());
        }
        user.setAvatarUrl(request.getAvatarUrl());

        userRepository.save(user);

        return UserDTO.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .password(null)
                .telephone(user.getTelephone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .etatCompte(user.getEtatCompte())
                .enabled(user.isEnabled())
                .dateDeNaissance(user.getDateDeNaissance())
                .niveau(user.getNiveau())
                .scoreTotal(user.getScoreTotal())
                .pointsExperience(user.getPointsExperience())
                .idRegion(user.getRegion() != null ? user.getRegion().getId() : null)
                .idPays(user.getRegion() != null && user.getRegion().getPays() != null ? user.getRegion().getPays().getId() : null)
                .onboardingCompleted(user.isOnboardingCompleted())
                .idGenre(user.getGenre() != null ? user.getGenre().getId() : null)
                .resetToken(user.getResetToken())
                .resetTokenExpiry(user.getResetTokenExpiry())
                .tokenVerification(user.getTokenVerification())
                .dateExpirationToken(user.getDateExpirationToken())
                .dateDerniereConnexion(user.getDateDerniereConnexion())
                .dateCreation(user.getDateCreation())
                .build();
    }
    @Transactional
    public void changePassword(Authentication authentication, ChangePasswordRequest request) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));

        // Vérifier mot de passe actuel
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw ApiException.badRequest("Mot de passe actuel incorrect");
        }

        // Vérifier que le nouveau mot de passe est différent
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw ApiException.badRequest("Le nouveau mot de passe doit être différent de l'ancien");
        }

        // Encoder nouveau mot de passe
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);
    }

    @Transactional
    public UserDTO completeOnboarding(Authentication authentication, PlayerOnboardingRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Corps de la requête invalide");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));

        if (user.getRole() != Role.JOUEUR) {
            throw ApiException.badRequest("L'onboarding n'est disponible que pour les joueurs");
        }

        String paysNom = request.getPaysNom() != null ? request.getPaysNom().trim() : null;
        String regionNom = request.getRegionNom() != null ? request.getRegionNom().trim() : null;

        if (paysNom == null || paysNom.isEmpty() || regionNom == null || regionNom.isEmpty()) {
            throw ApiException.badRequest("Pays et région sont requis");
        }

        Pays pays = paysRepository.findFirstByNomIgnoreCase(paysNom)
                .orElseGet(() -> {
                    Pays p = Pays.builder()
                            .nom(paysNom)
                            .codeIso(paysNom.length() >= 2 ? paysNom.substring(0, 2).toUpperCase() : "XX")
                            .build();
                    return paysRepository.save(p);
                });

        Region region = regionRepository.findFirstByPaysIdAndNomIgnoreCase(pays.getId(), regionNom)
                .orElseGet(() -> {
                    Region r = Region.builder().nom(regionNom).pays(pays).build();
                    return regionRepository.save(r);
                });

        user.setRegion(region);
        user.setAvatarUrl(request.getAvatarUrl());
        user.setOnboardingCompleted(true);
        userRepository.saveAndFlush(user);

        return buildUserDTO(user);
    }

    private UserDTO buildUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .password(null)
                .telephone(user.getTelephone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .etatCompte(user.getEtatCompte())
                .enabled(user.isEnabled())
                .dateDeNaissance(user.getDateDeNaissance())
                .niveau(user.getNiveau())
                .scoreTotal(user.getScoreTotal())
                .pointsExperience(user.getPointsExperience())
                .idRegion(user.getRegion() != null ? user.getRegion().getId() : null)
                .regionNom(user.getRegion() != null ? user.getRegion().getNom() : null)
                .idPays(user.getRegion() != null && user.getRegion().getPays() != null ? user.getRegion().getPays().getId() : null)
                .paysNom(user.getRegion() != null && user.getRegion().getPays() != null ? user.getRegion().getPays().getNom() : null)
                .onboardingCompleted(user.isOnboardingCompleted())
                .idGenre(user.getGenre() != null ? user.getGenre().getId() : null)
                .resetToken(user.getResetToken())
                .resetTokenExpiry(user.getResetTokenExpiry())
                .tokenVerification(user.getTokenVerification())
                .dateExpirationToken(user.getDateExpirationToken())
                .dateDerniereConnexion(user.getDateDerniereConnexion())
                .dateCreation(user.getDateCreation())
                .build();
    }
}
