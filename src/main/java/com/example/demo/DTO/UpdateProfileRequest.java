package com.example.demo.DTO;

import java.util.List;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String nom;
    private String prenom;
    private String email;
    private List<String> preferenceAlimentaire;
    private List<String> ingredientsApprecies;
    private List<String> ingredientsEvites;
    private List<String> contraintesAlimentaires;
}
