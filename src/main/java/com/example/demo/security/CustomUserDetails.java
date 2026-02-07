package com.example.demo.security;

import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.entitiesMysql.ennums.Role;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {
    private static final long serialVersionUID = 1L;    
    
    private final UserEntity userEntity;
    
    public CustomUserDetails(UserEntity userEntity) {
        this.userEntity = userEntity;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Role role = userEntity.getRole(); 
        
        if (role == null) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    // Supprimez la méthode normalizeRole qui est devenue inutile
    
    /**
     * Normalise les rôles pour correspondre à la configuration Spring Security
     */
    private String normalizeRole(String role) {
        if (role == null) {
            return "USER"; 
        }
        
        switch (role.toUpperCase()) {
            case "ADMINISTRATEUR":
                return "ADMIN";
            case "USER":
                return "USER";
            default:
                return role.toUpperCase();
        }
    }
    
    @Override
    public String getPassword() {
        return userEntity.getMotDePasse();
    }
    
    @Override
    public String getUsername() {
        return userEntity.getEmail();
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
    
    public Long getId() {
        return userEntity.getId();
    }
    
    public String getRole() {
        return userEntity.getRole() != null ? userEntity.getRole().name() : "USER";
    }
    
    public UserEntity getUserEntity() {
        return userEntity;
    }
}