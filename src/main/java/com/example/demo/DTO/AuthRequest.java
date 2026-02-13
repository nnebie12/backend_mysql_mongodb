package com.example.demo.DTO;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRequest {
	@NotBlank(message = "Le nom est requis")
    private String nom;
    
    @NotBlank(message = "Le prénom est requis")
    private String prenom;
    
    @NotBlank(message = "L'email est requis")
    @Email(message = "Email invalide")
    private String email;
    
    @NotBlank(message = "Le mot de passe est requis")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String motDePasse;
    
    private List<String> preferenceAlimentaire = new ArrayList<>();    
    private List<String> ingredientsApprecies = new ArrayList<>();
    
    private List<String> ingredientsEvites = new ArrayList<>();
    
    private List<String> contraintesAlimentaires = new ArrayList<>();
    
    
    @Pattern(regexp = "^(debutant|intermediaire|avance|expert)?$", 
             message = "Niveau de cuisine invalide")
    private String niveauCuisine;
    
    private Boolean newsletter = false;
}
