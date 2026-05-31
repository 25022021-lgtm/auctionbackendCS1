package com.auction.auth.jwtools;

import com.auction.users.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserDetailsImpl implements UserDetails {
    private String username;
    private String displayName;
    private Double balance;
    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(String username, String displayName, Double balance,
            Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.displayName = displayName;
        this.balance = balance;
        this.authorities = authorities;
    }

    public static UserDetailsImpl JPAtoUserDetails(User user) {
        List<GrantedAuthority> authorities;
        if ("admin".equals(user.getUsername())) {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities = List.of();
        }
        return new UserDetailsImpl(user.getUsername(), user.getDisplayName(), user.getBalance(), authorities);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return "";
    }

    public String getDisplayName() {
        return displayName;
    }

    public Double getBalance() {
        return balance;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
