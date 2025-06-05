package com.example.demo.controllerMysql;

import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.repositoryMysql.UserRepository;
import com.example.demo.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerIntegrationTest {

	@LocalServerPort
    private int port;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    private String baseUrl;

    private RestTemplate restTemplate = new RestTemplate();

    private UserEntity adminUser;
    private UserEntity normalUser;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/users";

        userRepository.deleteAll();

        adminUser = new UserEntity(null, "Admin", "User", "admin@test.com", "adminpass", null, "ADMIN", null);
        normalUser = new UserEntity(null, "Normal", "User", "user@test.com", "userpass", null, "USER", null);

        adminUser = userRepository.save(adminUser);
        normalUser = userRepository.save(normalUser);
    }

    @Test
    @DisplayName("✅ Accès autorisé avec token valide")
    void testGetAllUsersWithValidToken() {
        String token = jwtUtil.generateToken(adminUser.getEmail(), adminUser.getId(), adminUser.getRole());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/all",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("❌ Accès refusé avec token invalide")
    void testAccessDeniedWithInvalidToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid-token");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/all",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("❌ Un utilisateur ne peut pas supprimer un autre utilisateur")
    void testUserCannotDeleteOtherUser() {
        String token = jwtUtil.generateToken(normalUser.getEmail(), normalUser.getId(), normalUser.getRole());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + adminUser.getId(),
                HttpMethod.DELETE,
                request,
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
