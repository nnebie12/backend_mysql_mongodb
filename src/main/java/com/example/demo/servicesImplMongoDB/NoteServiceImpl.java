package com.example.demo.servicesImplMongoDB;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.DTO.NoteRequestDTO;
import com.example.demo.DTO.NoteResponseDTO;
import com.example.demo.entiesMongodb.NoteDocument;
import com.example.demo.repositoryMongoDB.NoteMongoRepository;
import com.example.demo.servicesMongoDB.NoteService;

@Service
public class NoteServiceImpl implements NoteService {

    private final NoteMongoRepository noteRepository;

    public NoteServiceImpl(NoteMongoRepository noteRepository ) {
        this.noteRepository = noteRepository;
    }

    private NoteResponseDTO convertToResponseDTO(NoteDocument document) {
        return new NoteResponseDTO(
            document.getId(),
            document.getValeur(),
            document.getUserId(),
            document.getRecetteId(),
            document.getUserName()
        );
    }

    @Override
    public List<NoteResponseDTO> getAllNotes() {
        return noteRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public NoteResponseDTO addNote(NoteRequestDTO noteDto) {

        NoteDocument noteDocument = new NoteDocument();
        noteDocument.setValeur(noteDto.getValeur());
        noteDocument.setUserId(noteDto.getUserId());
        noteDocument.setRecetteId(noteDto.getRecetteId());

       
        if (noteDocument.getUserName() == null || noteDocument.getUserName().isEmpty()) {
            noteDocument.setUserName("Anonymous"); 
        }


        NoteDocument savedNote = noteRepository.save(noteDocument);
        return convertToResponseDTO(savedNote);
    }

    @Override
    public Optional<NoteResponseDTO> getNoteById(String id) {
        return noteRepository.findById(id).map(this::convertToResponseDTO);
    }

    @Override
    public NoteResponseDTO updateNote(String id, NoteRequestDTO noteDto) {
        Optional<NoteDocument> existingNote = noteRepository.findById(id);

        if (existingNote.isPresent()) {
            NoteDocument updatedNote = existingNote.get();
            updatedNote.setValeur(noteDto.getValeur());
            

            NoteDocument savedNote = noteRepository.save(updatedNote);
            return convertToResponseDTO(savedNote);
        } else {
            throw new RuntimeException("Note not found with id: " + id);
        }
    }

    @Override
    public void deleteNote(String id) {
        noteRepository.deleteById(id);
    }
}