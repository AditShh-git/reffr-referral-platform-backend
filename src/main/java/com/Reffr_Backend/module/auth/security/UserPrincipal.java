package com.Reffr_Backend.module.auth.security;

import com.Reffr_Backend.module.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wraps a {@link User} entity as both a Spring Security {@link UserDetails}
 * and an {@link OAuth2User}, so it works in both JWT and OAuth2 flows.
 */
@Getter
public class UserPrincipal implements OAuth2User, UserDetails {

    private final UUID   id;
    private final String githubUsername;
    private final String githubId;
    private final Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    private UserPrincipal(UUID id, String githubUsername, String githubId,
                          Collection<? extends GrantedAuthority> authorities) {
        this.id             = id;
        this.githubUsername = githubUsername;
        this.githubId       = githubId;
        this.authorities    = authorities;
    }

    public static UserPrincipal from(User user) {
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        return new UserPrincipal(user.getId(), user.getGithubUsername(),
                user.getGithubId(), authorities);
    }

    public static UserPrincipal fromWithAttributes(User user, Map<String, Object> attributes) {
        UserPrincipal principal = from(user);
        principal.attributes = attributes;
        return principal;
    }

    // ── OAuth2User ────────────────────────────────────────────────────

    @Override
    public Map<String, Object> getAttributes() {
        return attributes != null ? attributes : Map.of();
    }

    @Override
    public String getName() {
        return githubUsername;
    }

    // ── UserDetails ───────────────────────────────────────────────────

    @Override public String getPassword()   { return null; }
    @Override public String getUsername()   { return githubUsername; }
    @Override public boolean isAccountNonExpired()   { return true; }
    @Override public boolean isAccountNonLocked()    { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()             { return true; }
}
