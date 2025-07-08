package com.example.demo.servicesMongoDB;

import java.util.List;
import java.util.Optional;

import com.example.demo.DTO.NoteRequestDTO;
import com.example.demo.DTO.NoteResponseDTO;

public interface NoteService {
    List<NoteResponseDTO> getAllNotes(); 
    NoteResponseDTO addNote(NoteRequestDTO noteDto); 
    Optional<NoteResponseDTO> getNoteById(String id);
    NoteResponseDTO updateNote(String id, NoteRequestDTO noteDto); 
    void deleteNote(String id);
}
