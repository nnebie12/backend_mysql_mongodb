package com.example.demo.controllerMysql;

import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.repositoryMysql.FavorisRepository;
import com.example.demo.repositoryMysql.RecetteRepository;
import com.example.demo.repositoryMysql.UserRepository;
import com.example.demo.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.jdbc.core.JdbcTemplate; 

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) 
@AutoConfigureMockMvc 
@TestPropertySource(properties = {
    "jwt.secret=0a8PXTxSL6b2mquKn8p2tKh2b6hOebQi75+3izNlqDzlggoNbLiPWbHnAw2wdlg4cLqVsmjzqd0rneAnC8IJ2A==",
    "jwt.expiration=86400000"
})
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc; 

    @Autowired
    private JwtUtil jwtUtil; 

    @Autowired
    private UserRepository userRepository; 
    @Autowired
    private FavorisRepository favorisRepository;

    @Autowired
    private RecetteRepository recetteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate; 

    private UserEntity adminUser;
    private UserEntity normalUser;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");

        // Nettoyer les tables dans l'ordre inverse des dépendances pour éviter les erreurs de clé étrangère
        favorisRepository.deleteAllInBatch(); 
        recetteRepository.deleteAllInBatch(); 
        userRepository.deleteAllInBatch();  

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        

        adminUser = new UserEntity(null, "Admin", "User", "admin@test.com", "adminpass", null, "ADMINISTRATEUR", null); 
        normalUser = new UserEntity(null, "Normal", "User", "user@test.com", "userpass", null, "USER", null);

        adminUser = userRepository.save(adminUser);
        normalUser = userRepository.save(normalUser);
        
        System.out.println("Admin user: " + adminUser);
        System.out.println("Normal user: " + normalUser);
    }

    @Test
    @DisplayName("❌ Accès refusé à /api/v1/users sans token (doit être 403 Forbidden)")
    void testAccessDeniedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("✅ Accès autorisé à /api/v1/users avec token valide (Admin)")
    void testGetAllUsersAsAdminWithValidToken() throws Exception {
        String adminToken = jwtUtil.generateToken(adminUser.getEmail(), adminUser.getId(), adminUser.getRole());
        System.out.println("Admin token: " + adminToken);

        MvcResult result = mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        
        System.out.println("Response: " + result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("❌ Accès refusé à /api/v1/users pour un utilisateur simple (doit être 403 Forbidden)")
    void testGetAllUsersAsNormalUser_Forbidden() throws Exception {
        String normalUserToken = jwtUtil.generateToken(normalUser.getEmail(), normalUser.getId(), normalUser.getRole());
        System.out.println("Normal user token: " + normalUserToken);

        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + normalUserToken))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("❌ Accès refusé avec token JWT invalide")
    void testAccessDeniedWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isForbidden()); 
    }

    @Test
    @DisplayName("❌ Un utilisateur simple ne peut pas supprimer un autre utilisateur (doit être 403 Forbidden)")
    void testNormalUserCannotDeleteOtherUser_Forbidden() throws Exception {
        String normalUserToken = jwtUtil.generateToken(normalUser.getEmail(), normalUser.getId(), normalUser.getRole());

        mockMvc.perform(delete("/api/v1/users/" + adminUser.getId())
                .header("Authorization", "Bearer " + normalUserToken))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("✅ Un administrateur peut supprimer n'importe quel utilisateur (doit être 200 OK)")
    void testAdminCanDeleteAnyUser() throws Exception {
        String adminToken = jwtUtil.generateToken(adminUser.getEmail(), adminUser.getId(), adminUser.getRole());

        mockMvc.perform(delete("/api/v1/users/" + normalUser.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
