package dev.nickbern.mercado_paramo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.limite-intentos.max=3")
class LimiteIntentosIntegrationTest extends IntegracionBase {

    private ResultActions intentarLogin(String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .with(peticion -> { peticion.setRemoteAddr(ip); return peticion; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"incorrecta-123"}
                        """.formatted(ADMIN_EMAIL)));
    }

    @Test
    void login_superaElLimite_devuelve429ConRetryAfter() throws Exception {
        String ip = "203.0.113.10";
        for (int i = 0; i < 3; i++) {
            intentarLogin(ip).andExpect(status().isUnauthorized());
        }

        intentarLogin(ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message", containsString("Demasiados intentos")));
    }

    @Test
    void login_bloqueado_noAfectaAOtraIp() throws Exception {
        String atacante = "203.0.113.20";
        for (int i = 0; i < 3; i++) {
            intentarLogin(atacante);
        }
        intentarLogin(atacante).andExpect(status().isTooManyRequests());

        intentarLogin("198.51.100.7").andExpect(status().isUnauthorized());
    }

    @Test
    void login_bloqueado_noBloqueaElCatalogo() throws Exception {
        String ip = "203.0.113.30";
        for (int i = 0; i < 4; i++) {
            intentarLogin(ip);
        }

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/categorias")
                        .with(peticion -> { peticion.setRemoteAddr(ip); return peticion; }))
                .andExpect(status().isOk());
    }

    @Test
    void registro_superaElLimite_devuelve429() throws Exception {
        String ip = "203.0.113.40";
        for (int i = 0; i < 3; i++) {
            final int n = i;
            mockMvc.perform(post("/api/auth/registro")
                            .with(peticion -> { peticion.setRemoteAddr(ip); return peticion; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombre":"Spam","email":"spam%d@correo.com","password":"%s","telefono":"300","rol":"COMPRADOR"}
                                    """.formatted(n, PASSWORD)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/auth/registro")
                        .with(peticion -> { peticion.setRemoteAddr(ip); return peticion; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Spam","email":"spam9@correo.com","password":"%s","telefono":"300","rol":"COMPRADOR"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isTooManyRequests());
    }
}