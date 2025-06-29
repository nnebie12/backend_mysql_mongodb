package com.example.demo.config;


import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.repositoryMysql.UserRepository;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail("dianekassi@admin.com").isEmpty()) {
            UserEntity admin = new UserEntity();
            admin.setNom("KASSI");
            admin.setPrenom("Diane");
            admin.setEmail("dianekassi@admin.com");
            admin.setMotDePasse(passwordEncoder.encode("Mydayana48"));
            admin.setRole("ADMINISTRATEUR");
            
            userRepository.save(admin);
            System.out.println("Utilisateur administrateur initial créé : dianekassi@admin.com");
        } else {
            System.out.println("Utilisateur administrateur existe déjà.");
        }
    }
}