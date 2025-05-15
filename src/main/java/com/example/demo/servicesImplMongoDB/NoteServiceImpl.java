package com.example.demo.servicesImplMongoDB;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.repositoryMongoDB.NoteMongoRepository;
import com.example.demo.servicesMongoDB.NoteService;

@Service
public class NoteServiceImpl implements NoteService {

    private final NoteMongoRepository noteRepository;

    public NoteServiceImpl(NoteMongoRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Override
    public List<NoteDocument> getAllNoteEntity() {
        return noteRepository.findAll();
    }

    @Override
    public NoteDocument addNoteEntity(NoteDocument noteEntity) {
        return noteRepository.save(noteEntity);
    }

    @Override
    public Optional<NoteDocument> getNoteById(Long id) {
        return noteRepository.findById(id.toString());
    }

    @Override
    public NoteDocument updateNote(Long id, NoteDocument noteEntity) {
        Optional<NoteDocument> existingNote = noteRepository.findById(id.toString());
        
        if (existingNote.isPresent()) {
            NoteDocument updatedNote = existingNote.get();
            updatedNote.setValeur(noteEntity.getValeur());
            // Ne pas modifier userId et userName pour maintenir l'intégrité
            
            return noteRepository.save(updatedNote);
        } else {
            throw new RuntimeException("Note not found with id: " + id);
        }
    }

    @Override
    public void deleteNote(Long id) {
        noteRepository.deleteById(id.toString());
    }
}