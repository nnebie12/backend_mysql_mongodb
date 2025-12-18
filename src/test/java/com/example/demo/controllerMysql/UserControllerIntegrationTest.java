package com.example.demo.controllerMysql;

import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.servicesMysql.UserService;
import com.example.demo.web.controllersMysql.UserController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;


@ExtendWith(MockitoExtension.class)
class UserControllerUnitTest {

    @Mock
    private UserService userService;
    
    @InjectMocks
    private UserController userController;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    private UserEntity adminUser;
    private UserEntity normalUser;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        objectMapper = new ObjectMapper();
        
        adminUser = new UserEntity(1L, "Admin", "User", "admin@test.com", "adminpass", null, "ADMINISTRATEUR", null);
        normalUser = new UserEntity(2L, "Normal", "User", "user@test.com", "userpass", null, "USER", null);
    }

    @Test
    @DisplayName(" getAllUsers - Doit retourner la liste des utilisateurs")
    void testGetAllUsers_ShouldReturnUsersList() throws Exception {
        List<UserEntity> users = Arrays.asList(adminUser, normalUser);
        when(userService.getAllUsers()).thenReturn(users);
        
        mockMvc.perform(get("/api/v1/users")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("admin@test.com"))
                .andExpect(jsonPath("$[1].email").value("user@test.com"));
                
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName(" getUserById - Doit retourner l'utilisateur trouvé")
    void testGetUserById_ShouldReturnUser() throws Exception {
        when(userService.getUserById(1L)).thenReturn(Optional.of(adminUser));
        
        mockMvc.perform(get("/api/v1/users/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("admin@test.com"))
                .andExpect(jsonPath("$.role").value("ADMINISTRATEUR"));
                
        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName(" getUserById - Doit retourner 404 si utilisateur non trouvé")
    void testGetUserById_ShouldReturn404WhenUserNotFound() throws Exception {
        when(userService.getUserById(999L)).thenReturn(Optional.empty());
        
        mockMvc.perform(get("/api/v1/users/999")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
                
        verify(userService, times(1)).getUserById(999L);
    }

    @Test
    @DisplayName(" updateUser - Doit mettre à jour un utilisateur existant")
    void testUpdateUser_ShouldUpdateExistingUser() throws Exception {
        
        UserEntity updatedUser = new UserEntity(1L, "User", "Updated", "updated@test.com", "updatedpass", null, "USER", null);
        
        when(userService.updateUserAsAdmin(eq(1L), any(UserEntity.class))).thenReturn(updatedUser);
        
        mockMvc.perform(put("/api/v1/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("updated@test.com"))
                .andExpect(jsonPath("$.prenom").value("Updated"))  
                .andExpect(jsonPath("$.nom").value("User"));       
                
        verify(userService, times(1)).updateUserAsAdmin(eq(1L), any(UserEntity.class));
    }

    @Test
    @DisplayName(" updateUser - Doit retourner 404 si utilisateur non trouvé")
    void testUpdateUser_ShouldReturn404WhenUserNotFound() throws Exception {
        UserEntity updatedUser = new UserEntity(999L, "Updated", "User", "updated@test.com", "updatedpass", null, "USER", null);
        
        when(userService.updateUserAsAdmin(eq(999L), any(UserEntity.class)))
                .thenThrow(new UserNotFoundException("User not found with id: 999"));
        
        mockMvc.perform(put("/api/v1/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isNotFound());
                
        verify(userService, times(1)).updateUserAsAdmin(eq(999L), any(UserEntity.class));
    }

    @Test
    @DisplayName(" updateUser - Doit retourner 409 en cas de conflit de données")
    void testUpdateUser_ShouldReturn409OnDataIntegrityViolation() throws Exception {
        UserEntity updatedUser = new UserEntity(1L, "Updated", "User", "updated@test.com", "updatedpass", null, "USER", null);
        
        when(userService.updateUserAsAdmin(eq(1L), any(UserEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate email"));
        
        mockMvc.perform(put("/api/v1/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isConflict());
                
        verify(userService, times(1)).updateUserAsAdmin(eq(1L), any(UserEntity.class));
    }

    @Test
    @DisplayName(" updateUser - Doit retourner 500 en cas d'erreur inattendue")
    void testUpdateUser_ShouldReturn500OnUnexpectedError() throws Exception {
        UserEntity updatedUser = new UserEntity(1L, "Updated", "User", "updated@test.com", "updatedpass", null, "USER", null);
        
        when(userService.updateUserAsAdmin(eq(1L), any(UserEntity.class)))
                .thenThrow(new RuntimeException("Unexpected error"));
        
        mockMvc.perform(put("/api/v1/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isInternalServerError());
                
        verify(userService, times(1)).updateUserAsAdmin(eq(1L), any(UserEntity.class));
    }

    @Test
    @DisplayName(" deleteUser - Doit supprimer un utilisateur existant")
    void testDeleteUser_ShouldDeleteExistingUser() throws Exception {
        doNothing().when(userService).deleteUser(1L);
        
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isOk());
                
        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    @DisplayName(" deleteUser - Doit retourner 404 si utilisateur non trouvé")
    void testDeleteUser_ShouldReturn404WhenUserNotFound() throws Exception {
        doThrow(new UserNotFoundException("User not found with id: 999"))
                .when(userService).deleteUser(999L);
        
        mockMvc.perform(delete("/api/v1/users/999"))
                .andExpect(status().isNotFound());
                
        verify(userService, times(1)).deleteUser(999L);
    }

    @Test
    @DisplayName(" deleteUser - Doit retourner 409 en cas de conflit de données")
    void testDeleteUser_ShouldReturn409OnDataIntegrityViolation() throws Exception {
        doThrow(new DataIntegrityViolationException("Cannot delete user with associated data"))
                .when(userService).deleteUser(1L);
        
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isConflict());
                
        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    @DisplayName(" deleteUser - Doit retourner 500 en cas d'erreur inattendue")
    void testDeleteUser_ShouldReturn500OnUnexpectedError() throws Exception {
        doThrow(new RuntimeException("Unexpected error"))
                .when(userService).deleteUser(1L);
        
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isInternalServerError());
                
        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    @DisplayName(" Service mock - Vérifier que les mocks fonctionnent correctement")
    void testServiceMock_ShouldWorkCorrectly() {
        when(userService.getUserById(1L)).thenReturn(Optional.of(adminUser));
        
        Optional<UserEntity> result = userService.getUserById(1L);
        
        assert result.isPresent();
        assert result.get().getEmail().equals("admin@test.com");
        verify(userService, times(1)).getUserById(1L);
    }
}