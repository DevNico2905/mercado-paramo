package dev.nickbern.mercado_paramo.service;

import dev.nickbern.mercado_paramo.dto.CategoriaResponse;
import dev.nickbern.mercado_paramo.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll(Sort.by("nombre")).stream()
                .map(CategoriaResponse::de)
                .toList();
    }
}