package com.barangay.clearance.shared.security;

import com.barangay.clearance.identity.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final User.Role role;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID userId, String email, User.Role role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // not used — password check done in AuthService
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
