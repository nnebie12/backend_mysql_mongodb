package com.example.demo.security;

import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.servicesMysql.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<UserEntity> userOpt = userService.getUserByEmail(email);
        if (userOpt.isPresent()) {
            return new CustomUserDetails(userOpt.get());
        }
        throw new UsernameNotFoundException("Utilisateur non trouvé avec l'email: " + email);
    }
}