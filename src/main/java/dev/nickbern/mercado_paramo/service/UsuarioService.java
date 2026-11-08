package dev.nickbern.mercado_paramo.service;

import dev.nickbern.mercado_paramo.dto.PerfilResponse;
import dev.nickbern.mercado_paramo.exception.RecursoNoEncontradoException;
import dev.nickbern.mercado_paramo.model.Usuario;
import dev.nickbern.mercado_paramo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public PerfilResponse perfil(String email){

        Usuario usuario_db = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return PerfilResponse.builder()
                .id(usuario_db.getId())
                .nombre(usuario_db.getNombre())
                .email(usuario_db.getEmail())
                .rol(usuario_db.getRol())
                .build();
    }

}
