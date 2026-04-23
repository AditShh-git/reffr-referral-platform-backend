package com.Reffr_Backend.module.auth.security;

import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Loads (or auto-registers) a user after GitHub OAuth2 authentication.
 * Returns a {@link UserPrincipal} so the rest of the security pipeline
 * works with our domain model rather than raw OAuth2 attributes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String githubId       = String.valueOf(attributes.get("id"));
        String githubUsername = (String) attributes.get("login");
        String name           = (String) attributes.getOrDefault("name", githubUsername);
        String avatarUrl      = (String) attributes.get("avatar_url");
        String githubUrl      = (String) attributes.get("html_url");

        //  Fix: handle email properly
        String rawEmail = (String) attributes.get("email");

        final String email = (rawEmail == null || rawEmail.isBlank())
                ? githubUsername + "@users.noreply.github.com"
                : rawEmail;

        User user = userRepository.findByGithubId(githubId)
                .map(existing -> updateExistingUser(existing, name, avatarUrl, githubUrl))
                .orElseGet(() -> createNewUser(githubId, githubUsername, name, avatarUrl, githubUrl));

        log.info("OAuth2 login: {} ({})", githubUsername, user.getId());
        return UserPrincipal.fromWithAttributes(user, attributes);
    }

    private User updateExistingUser(User user, String name, String avatarUrl, String githubUrl) {

        user.setName(name != null ? name : user.getName());
        user.setAvatarUrl(avatarUrl);
        user.setGithubUrl(githubUrl);

        return userRepository.save(user);
    }

    private User createNewUser(String githubId, String githubUsername, String name,
                               String avatarUrl, String githubUrl) {

        //  Fix 2: Prevent username conflict
        String finalUsername = githubUsername;

        if (userRepository.existsByGithubUsername(githubUsername)) {
            finalUsername = githubUsername + "_" + System.currentTimeMillis();
        }

        User user = User.builder()
                .githubId(githubId)
                .githubUsername(finalUsername)
                .name(name != null ? name : finalUsername)
                .avatarUrl(avatarUrl)
                .githubUrl(githubUrl)
                .build();

        return userRepository.save(user);
    }
}