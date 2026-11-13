package dev.nickbern.mercado_paramo.integration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String REGISTRO_JSON = """
            {"nombre":"María","email":"maria@correo.com","password":"clave-segura-123",
             "telefono":"3001234567","rol":"COMPRADOR"}
            """;

    private static final String LOGIN_JSON = """
            {"email":"maria@correo.com","password":"clave-segura-123"}
            """;


    private void registrarMaria() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isCreated());
    }

    private String registrarYObtenerToken() throws Exception {
        registrarMaria();

        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(respuesta, "$.token");
    }

    @Test
    void me_conTokenValido_devuelvePerfil() throws Exception {
        String token = registrarYObtenerToken();

        mockMvc.perform(get("/api/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("maria@correo.com"))
                .andExpect(jsonPath("$.nombre").value("María"))
                .andExpect(jsonPath("$.rol").value("COMPRADOR"))
                .andExpect(jsonPath("$.estadoVerificacion").value(nullValue()))
                .andExpect(jsonPath("$.vereda").value(nullValue()));
    }

    @Test
    void me_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Se requiere autenticación"));
    }

    @Test
    void me_conTokenAlterado_devuelve401() throws Exception {
        String token = registrarYObtenerToken();
        // Cambia un carácter en medio de la firma (no el último: ver explicación)
        int posicion = token.lastIndexOf('.') + 10;
        char original = token.charAt(posicion);
        char reemplazo = (original == 'a') ? 'b' : 'a';
        String tokenAlterado = token.substring(0, posicion) + reemplazo + token.substring(posicion + 1);

        mockMvc.perform(get("/api/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Se requiere autenticación"));
    }

    @Test
    void me_sinPrefijoBearer_devuelve401() throws Exception {
        String token = registrarYObtenerToken();

        mockMvc.perform(get("/api/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registro_emailInvalido_devuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"María","email":"no-es-email","password":"clave-segura-123",
                                 "telefono":"3001234567","rol":"COMPRADOR"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.email").exists());
    }

    @Test
    void login_credencialesInvalidas_devuelve401() throws Exception {
        registrarMaria();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"maria@correo.com","password":"incorrecta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    void login_conTokenInvalidoEnHeader_igualPermiteIniciarSesion() throws Exception {
        registrarMaria();

        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-viejo-o-basura")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void me_conBearerVacio_devuelve401() throws Exception {
        mockMvc.perform(get("/api/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Se requiere autenticación"));
    }

    @Test
    void me_productorRecienRegistrado_devuelveEstadoPendiente() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nombre":"Pedro","email":"pedro@correo.com","password":"clave-segura-123",
                             "telefono":"3009876543","rol":"PRODUCTOR","vereda":"La Concepción"}
                            """))
                .andExpect(status().isCreated());

        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"pedro@correo.com","password":"clave-segura-123"}
                            """))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(respuesta, "$.token");

        mockMvc.perform(get("/api/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("PRODUCTOR"))
                .andExpect(jsonPath("$.estadoVerificacion").value("PENDIENTE"))
                .andExpect(jsonPath("$.vereda").value("La Concepción"));
    }
}