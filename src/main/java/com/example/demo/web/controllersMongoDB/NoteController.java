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

import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.servicesMongoDB.NoteService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    @Autowired
    private NoteService noteService;
    
    @GetMapping("/test")
    @Operation(summary = "Tester l'API", description = "Retourne un message de test")
    public String test() {
        return "Note API Functional ok";
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<NoteDocument>> getAllNotes() {
        List<NoteDocument> notes = noteService.getAllNoteEntity();
        return new ResponseEntity<>(notes, HttpStatus.OK);
    }
    
    @PostMapping
    public ResponseEntity<NoteDocument> createNote(@RequestBody NoteDocument note) {
        NoteDocument savedNote = noteService.addNoteEntity(note);
        return new ResponseEntity<>(savedNote, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<NoteDocument> getNoteById(@PathVariable Long id) {
        return noteService.getNoteById(id)
                .map(note -> new ResponseEntity<>(note, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<NoteDocument> updateNote(@PathVariable Long id, @RequestBody NoteDocument noteDetails) {
        try {
            NoteDocument updatedNote = noteService.updateNote(id, noteDetails);
            return new ResponseEntity<>(updatedNote, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        try {
            noteService.deleteNote(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}