package com.example.demo.web.controllersMongoDB;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.example.demo.entiesMongodb.CommentaireDocument;
import com.example.demo.servicesMongoDB.CommentaireService;

@RestController
@RequestMapping("/api/v1/commentaires")
public class CommentaireController {

    @Autowired
    private CommentaireService commentaireService;
    
    @GetMapping("/all")
    public ResponseEntity<List<CommentaireDocument>> getAllCommentaires() {
        List<CommentaireDocument> commentaires = commentaireService.getAllCommentaireEntity();
        return new ResponseEntity<>(commentaires, HttpStatus.OK);
    }
    
    @PostMapping
    public ResponseEntity<CommentaireDocument> createCommentaire(@RequestBody CommentaireDocument commentaire) {
        CommentaireDocument savedCommentaire = commentaireService.addCommentaireEntity(commentaire);
        return new ResponseEntity<>(savedCommentaire, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CommentaireDocument> getCommentaireById(@PathVariable Long id) {
        return commentaireService.getCommentaireById(id)
                .map(commentaire -> new ResponseEntity<>(commentaire, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CommentaireDocument> updateCommentaire(@PathVariable Long id, @RequestBody CommentaireDocument commentaireDetails) {
        try {
            CommentaireDocument updatedCommentaire = commentaireService.updateCommentaire(id, commentaireDetails);
            return new ResponseEntity<>(updatedCommentaire, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommentaire(@PathVariable Long id) {
        try {
            commentaireService.deleteCommentaire(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}