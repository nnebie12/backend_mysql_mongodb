package com.example.demo.web.controllersMongoDB;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTO.CommentaireRequestDTO;
import com.example.demo.DTO.CommentaireResponseDTO;
import com.example.demo.servicesMongoDB.CommentaireService;

@RestController 
@RequestMapping("/api/v1/commentaires")
public class CommentaireController {

    private final CommentaireService commentaireService;

    public CommentaireController(CommentaireService commentaireService) {
        this.commentaireService = commentaireService;
    }

    @GetMapping("/all") 
    public ResponseEntity<List<CommentaireResponseDTO>> getAllCommentaires() {
        List<CommentaireResponseDTO> commentaires = commentaireService.getAllCommentaires();
        return new ResponseEntity<>(commentaires, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<CommentaireResponseDTO> createCommentaire(@RequestBody CommentaireRequestDTO commentaireDto) {
        CommentaireResponseDTO savedCommentaire = commentaireService.addCommentaire(commentaireDto);
        return new ResponseEntity<>(savedCommentaire, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentaireResponseDTO> getCommentaireById(@PathVariable String id) {
        return commentaireService.getCommentaireById(id)
                .map(commentaire -> new ResponseEntity<>(commentaire, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}") 
    public ResponseEntity<CommentaireResponseDTO> updateCommentaire(@PathVariable String id, @RequestBody CommentaireRequestDTO commentaireDetailsDto) {
        try {
            CommentaireResponseDTO updatedCommentaire = commentaireService.updateCommentaire(id, commentaireDetailsDto);
            return new ResponseEntity<>(updatedCommentaire, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}") 
    public ResponseEntity<Void> deleteCommentaire(@PathVariable String id) {
        try {
            commentaireService.deleteCommentaire(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); 
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}