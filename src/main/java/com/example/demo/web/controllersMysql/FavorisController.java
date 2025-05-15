package com.example.demo.web.controllersMysql;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entitiesMysql.FavorisEntity;
import com.example.demo.servicesMysql.FavorisService;

@RestController
@RequestMapping("/api/favoris")
public class FavorisController {
    private final FavorisService favorisService;
    
    public FavorisController(FavorisService favorisService) {
        this.favorisService = favorisService;
    }
    
    @PostMapping("/{userId}/{recetteId}")
    public ResponseEntity<FavorisEntity> addFavori(@PathVariable Long userId, @PathVariable Long recetteId) {
    	FavorisEntity favorisEntity = favorisService.addFavori(userId, recetteId);
        return new ResponseEntity<>(favorisEntity, HttpStatus.CREATED);
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<List<FavorisEntity>> getFavorisByUserId(@PathVariable Long userId) {
        List<FavorisEntity> favorisEntity = favorisService.getFavorisByUserId(userId);
        return new ResponseEntity<>(favorisEntity, HttpStatus.OK);
    }
    
    @DeleteMapping("/{userId}/{recetteId}")
    public ResponseEntity<Void> deleteFavori(@PathVariable Long userId, @PathVariable Long recetteId) {
        favorisService.deleteFavori(userId, recetteId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
