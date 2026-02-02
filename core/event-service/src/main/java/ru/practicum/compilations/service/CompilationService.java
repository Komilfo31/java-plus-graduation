package ru.practicum.compilations.service;

import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.dto.UpdateCompilationRequest;
import ru.practicum.compilations.params.PublicCompilationsParams;

import java.util.List;

public interface CompilationService {

    //public
    List<CompilationDto> findCompilations(PublicCompilationsParams params);

    CompilationDto findCompilationById(Integer compId);

    //admin
    CompilationDto addCompilation(NewCompilationDto dto);

    CompilationDto updateCompilation(Integer compId, UpdateCompilationRequest dto);

    void removeCompilation(Integer compId);

}
