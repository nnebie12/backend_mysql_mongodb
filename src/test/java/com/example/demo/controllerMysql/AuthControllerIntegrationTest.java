package com.example.demo.controllerMysql;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.DTO.AuthRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthRequest buildRequest(String email) {
        AuthRequest request = new AuthRequest();
        request.setNom("Doe");
        request.setPrenom("John");
        request.setEmail(email);
        request.setMotDePasse("securePassword123");
        request.setNiveauCuisine("debutant");
        request.setNewsletter(false);
        return request;
    }

    @Test
    public void testRegisterNewUser_success() throws Exception {
        String email = "john." + UUID.randomUUID() + "@example.com";
        AuthRequest request = buildRequest(email);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    public void testRegisterWithExistingEmail_conflict() throws Exception {
        String email = "duplicate." + UUID.randomUUID() + "@example.com";
        AuthRequest request = buildRequest(email);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
