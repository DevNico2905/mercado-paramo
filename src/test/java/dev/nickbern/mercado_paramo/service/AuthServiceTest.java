package dev.nickbern.mercado_paramo.service;

import dev.nickbern.mercado_paramo.dto.RegistroRequest;
import dev.nickbern.mercado_paramo.dto.RegistroResponse;
import dev.nickbern.mercado_paramo.exception.RecursoDuplicadoException;
import dev.nickbern.mercado_paramo.exception.ReglaNegocioException;
import dev.nickbern.mercado_paramo.model.EstadoVerificacion;
import dev.nickbern.mercado_paramo.model.Productor;
import dev.nickbern.mercado_paramo.model.Rol;
import dev.nickbern.mercado_paramo.model.Usuario;
import dev.nickbern.mercado_paramo.repository.ProductorRepository;
import dev.nickbern.mercado_paramo.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProductorRepository productorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registro_emailDuplicado_lanzaExcepcionYNoGuarda() {

        RegistroRequest request = new RegistroRequest(
                "María", "maria@correo.com", "clave-segura-123",
                "3001234567", Rol.COMPRADOR, null, null);

        when(usuarioRepository.existsByEmail("maria@correo.com")).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class,
                () -> authService.registro(request));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registro_rolAdmin_lanzaReglaNegocioYNoGuarda(){
        RegistroRequest request = new RegistroRequest(
                "María", "maria@correo.com", "clave-segura-123",
                "3001234567", Rol.ADMIN, null, null);

        assertThrows(ReglaNegocioException.class,
                () -> authService.registro(request));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registro_compradorValido_guardaUsuarioConPasswordCifradaYSinProductor() {

        RegistroRequest request = new RegistroRequest(
                "María", "maria@correo.com", "clave-segura-123",
                "3001234567", Rol.COMPRADOR, null, null);

        when(passwordEncoder.encode("clave-segura-123")).thenReturn("hash-falso");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        RegistroResponse response = authService.registro(request);

        assertEquals(Rol.COMPRADOR, response.rol());
        assertEquals("maria@correo.com", response.email());
        assertNull(response.estadoVerificacion());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("hash-falso", captor.getValue().getPasswordHash());

        verify(productorRepository, never()).save(any());
    }

    @Test
    void registro_productorSinVereda_lanzaReglaNegocio() {
        RegistroRequest request = new RegistroRequest(
                "Pedro", "pedro@correo.com", "clave-segura-123",
                "3009876543", Rol.PRODUCTOR, "   ", null);

        assertThrows(ReglaNegocioException.class,
                () -> authService.registro(request));

        verify(usuarioRepository, never()).save(any());
        verify(productorRepository, never()).save(any());
    }

    @Test
    void registro_productorValido_creaProductorPendiente() {

        RegistroRequest request = new RegistroRequest(
                "Pedro", "pedro@correo.com", "clave-segura-123",
                "3009876543", Rol.PRODUCTOR, "La Concepción", "Quesos de la finca El Roble");

        when(passwordEncoder.encode("clave-segura-123")).thenReturn("hash-falso");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(productorRepository.save(any(Productor.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        RegistroResponse response = authService.registro(request);

        assertEquals(Rol.PRODUCTOR, response.rol());
        assertEquals(EstadoVerificacion.PENDIENTE, response.estadoVerificacion());

        ArgumentCaptor<Productor> captor = ArgumentCaptor.forClass(Productor.class);
        verify(productorRepository).save(captor.capture());
        Productor productorGuardado = captor.getValue();

        assertEquals("La Concepción", productorGuardado.getVereda());
        assertEquals(EstadoVerificacion.PENDIENTE, productorGuardado.getEstadoVerificacion());
        assertNotNull(productorGuardado.getUsuario());
    }
}
