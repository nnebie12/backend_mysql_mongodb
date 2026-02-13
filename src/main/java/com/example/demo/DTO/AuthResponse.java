package com.example.demo.DTO;


import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private List<String> preferenceAlimentaire;
    private List<String> contraintesAlimentaires;
    private String niveauCuisine;
}