package com.example.demo.security;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthRequiredAspect {

    @Autowired
    private JwtUtil jwtUtil;

    @Around("@annotation(AuthRequired)")
    public Object checkAuthentication(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Pas de contexte de requête");
        }

        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token manquant");
        }

        String token = authHeader.substring(7);
        
        try {
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token invalide");
            }

            String email = jwtUtil.extractEmail(token);
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);

            request.setAttribute("email", email);
            request.setAttribute("userId", userId);
            request.setAttribute("role", role);

            return joinPoint.proceed();
        } catch (Exception e) {
            System.err.println("Erreur d'authentification: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Erreur d'authentification: " + e.getMessage());
        }
    }
}