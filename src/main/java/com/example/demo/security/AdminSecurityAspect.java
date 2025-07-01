package com.example.demo.security;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
public class AdminSecurityAspect {


    private final JwtUtil jwtUtil;

    public AdminSecurityAspect(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Before("@annotation(AdminRequired)")
    public void checkAdminRole() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        String tokenHeader = attributes.getRequest().getHeader("Authorization");


        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token manquant");
        }

        String token = tokenHeader.replace("Bearer ", "");

        String role = null;
        try {
            role = jwtUtil.extractRole(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Jeton invalide");
        }

        if (!"ADMINISTRATEUR".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès interdit - Droits administrateur requis");
        }
    }
}