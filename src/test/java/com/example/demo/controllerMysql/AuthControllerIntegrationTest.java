package com.example.demo.controllerMysql;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.entitiesMysql.ennums.Role;
import com.example.demo.repositoryMysql.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; 
    
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll(); 
    }

    @Test
    public void testRegisterNewUser_success() throws Exception {
        UserEntity user = new UserEntity();
        user.setNom("Doe");
        user.setPrenom("John");
        user.setEmail("john.doe@example.com");
        user.setMotDePasse("securePassword123");
        user.setPreferenceAlimentaire(Arrays.asList("Végétarien"));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated());
    }

    @Test
    public void testRegisterWithExistingEmail_conflict() throws Exception {
        UserEntity existing = new UserEntity(null, "Test", "User", "duplicate@example.com", "pass", 
            Arrays.asList("Vegan"), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null, false, Role.USER, null);
        existing = userRepository.save(existing); 
        userRepository.save(existing);

        UserEntity newUser = new UserEntity(null, "New", "User", "duplicate@example.com", "pass", 
            Arrays.asList("Vegan"), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null, false, Role.USER, null);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isConflict());
    
}
}
