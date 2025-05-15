package com.example.demo.web.controllersMysql;

import com.example.demo.entitiesMysql.RecetteEntity;
import com.example.demo.entitiesMysql.UserEntity;
import com.example.demo.security.JwtUtil;
import com.example.demo.servicesMysql.RecetteService;
import com.example.demo.servicesMysql.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final RecetteService recetteService;
    private final JwtUtil jwtUtil;

    public AdminController(UserService userService, RecetteService recetteService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.recetteService = recetteService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUtilisateurs(@RequestHeader("Authorization") String tokenHeader) {
        String token = tokenHeader.replace("Bearer ", "");
        String role = jwtUtil.extractRole(token);

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès interdit");
        }

        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{id}")
    public UserEntity updateUtilisateur(@PathVariable Long id, @RequestBody UserEntity updatedUser) {
        return userService.updateUser(id, updatedUser);
    }

    @DeleteMapping("/users/{id}")
    public void deleteUtilisateur(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @PostMapping("/recettes")
    public RecetteEntity createRecette(@RequestBody RecetteEntity recette) {
        throw new UnsupportedOperationException("Ajout recette sans userId non pris en charge");
    }

    @PutMapping("/recettes/{id}")
    public RecetteEntity updateRecette(@PathVariable Long id, @RequestBody RecetteEntity recette) {
        return recetteService.updateRecette(id, recette);
    }

    @DeleteMapping("/recettes/{id}")
    public void deleteRecette(@PathVariable Long id) {
        recetteService.deleteRecette(id);
    }
}
