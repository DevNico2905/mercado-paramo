package dev.nickbern.mercado_paramo.service;

import dev.nickbern.mercado_paramo.dto.PerfilResponse;
import dev.nickbern.mercado_paramo.exception.RecursoNoEncontradoException;
import dev.nickbern.mercado_paramo.model.EstadoVerificacion;
import dev.nickbern.mercado_paramo.model.Productor;
import dev.nickbern.mercado_paramo.model.Rol;
import dev.nickbern.mercado_paramo.model.Usuario;
import dev.nickbern.mercado_paramo.repository.ProductorRepository;
import dev.nickbern.mercado_paramo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ProductorRepository productorRepository;

    @Transactional(readOnly = true)
    public PerfilResponse perfil(String email) {

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Optional<Productor> productor = (usuario.getRol() == Rol.PRODUCTOR)
                ? productorRepository.findByUsuarioEmail(email)
                : Optional.empty();

        return PerfilResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .estadoVerificacion(productor.map(Productor::getEstadoVerificacion).orElse(null))
                .vereda(productor.map(Productor::getVereda).orElse(null))
                .build();
    }
}
