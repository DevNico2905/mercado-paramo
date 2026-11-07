package dev.nickbern.mercado_paramo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRETO = "dGVzdC1zZWNyZXQtbWVyY2Fkby1wYXJhbW8tMzItYnl0ZXM=";

    private JwtService jwtService;
    private UserDetails maria;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRETO, 60_000);   // tokens de 1 minuto
        maria = User.withUsername("maria@correo.com")
                .password("x")
                .roles("PRODUCTOR")
                .build();
    }

    @Test
    void generarToken_extraerEmail_devuelveElEmailDelUsuario() {
        String token = jwtService.generarToken(maria);

        assertEquals("maria@correo.com", jwtService.extraerEmail(token));
    }

    @Test
    void esValido_tokenDelMismoUsuario_devuelveTrueYParaOtroFalse() {
        UserDetails pedro = User.withUsername("pedro@correo.com")
                .password("x")
                .roles("COMPRADOR")
                .build();

        String token = jwtService.generarToken(maria);

        assertTrue(jwtService.esValido(token, maria));
        assertFalse(jwtService.esValido(token, pedro));
    }

    @Test
    void esValido_tokenAlterado_devuelveFalse() {
        String token = jwtService.generarToken(maria);

        int inicioFirma = token.lastIndexOf('.') + 1;
        char original = token.charAt(inicioFirma);
        char distinto = (original == 'A') ? 'B' : 'A';
        String alterado = token.substring(0, inicioFirma) + distinto + token.substring(inicioFirma + 1);

        assertFalse(jwtService.esValido(alterado, maria));
    }

    @Test
    void esValido_tokenVencido_devuelveFalse() {
        JwtService servicioVencido = new JwtService(SECRETO, -1000);   // vence 1 s antes de crearse

        String token = servicioVencido.generarToken(maria);

        assertFalse(servicioVencido.esValido(token, maria));
    }
}
