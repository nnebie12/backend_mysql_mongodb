package com.example.demo.servicesMongoDB;

import java.util.List;
import java.util.Optional;

import com.example.demo.entiesMongodb.NoteDocument;

public interface NoteService {

	List<NoteDocument> getAllNoteEntity();
    NoteDocument addNoteEntity(NoteDocument noteEntity);
    Optional<NoteDocument> getNoteById(Long id);
    NoteDocument updateNote(Long id, NoteDocument noteEntity);
    void deleteNote(Long id);
}
