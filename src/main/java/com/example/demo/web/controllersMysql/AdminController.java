package com.example.demo.web.controllersMysql;

import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.servicesMysql.RecetteService;
import com.example.demo.servicesMysql.UserService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    private final UserService userService;
    private final RecetteService recetteService;
    
    public AdminController(UserService userService, RecetteService recetteService) {
        this.userService = userService;
        this.recetteService = recetteService;
    }
    
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserEntity>> getAllUtilisateurs() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserEntity> updateUtilisateur(@PathVariable Long id, @RequestBody UserEntity updatedUser) {
        try {
            UserEntity user = userService.updateUserAsAdmin(id, updatedUser);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUtilisateur(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/recettes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecetteEntity> createRecette(@RequestBody RecetteEntity recetteEntity, @PathVariable Long userId) {
        try {
            RecetteEntity savedRecette = recetteService.saveRecette(recetteEntity, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedRecette);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/recettes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecetteEntity> updateRecette(@PathVariable Long id, @RequestBody RecetteEntity recette) {
        try {
            RecetteEntity updatedRecette = recetteService.updateRecette(id, recette);
            return ResponseEntity.ok(updatedRecette);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/recettes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRecette(@PathVariable Long id) {
        try {
            recetteService.deleteRecette(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}

