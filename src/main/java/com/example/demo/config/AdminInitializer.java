package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.entitiesMysql.ennums.Role;
import com.example.demo.repositoryMysql.UserRepository;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            UserEntity admin = new UserEntity();
            admin.setNom("KASSI"); 
            admin.setPrenom("Diane"); 
            admin.setEmail(adminEmail);
            String encodedPwd = passwordEncoder.encode(adminPassword);
            admin.setMotDePasse(encodedPwd);
            
            admin.setRole(Role.ADMINISTRATEUR);
            admin.setActif(true);

            userRepository.save(admin);
            logger.info("Utilisateur administrateur créé : {}", adminEmail);
        }
    }
}