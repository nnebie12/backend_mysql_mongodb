package com.example.demo.servicesMongoDB;

import java.util.List;
import java.util.Optional;

import com.example.demo.entiesMongodb.NoteDocument;

public interface NoteService {

	List<NoteDocument> getAllNoteEntity();
    NoteDocument addNoteEntity(NoteDocument noteEntity);
    Optional<NoteDocument> getNoteById(String id);
    NoteDocument updateNote(String id, NoteDocument noteEntity);
    void deleteNote(String id);

}
