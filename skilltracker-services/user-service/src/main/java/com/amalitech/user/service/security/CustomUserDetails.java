package com.amalitech.user.service.security;

import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.enums.UserState;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {
    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * An account is considered "non-locked" unless it is explicitly
     * in a state that implies a lock (e.g., SUSPENDED).
     */
    @Override
    public boolean isAccountNonLocked() {
        return user.getState() != UserState.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * An account should be enabled for all valid, active states.
     */
    @Override
    public boolean isEnabled() {
        return user.getState() != UserState.SUSPENDED;
    }
}
