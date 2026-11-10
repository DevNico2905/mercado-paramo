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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminProductoresIntegrationTest {

    private static final String URL = "/api/admin/productores";

    @Autowired
    private MockMvc mockMvc;

    private String login(String email, String password) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private String tokenAdmin() throws Exception {
        return login("admin@mercado-paramo.local", "admin-test-123");
    }

    private void registrarPedroProductor() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Pedro","email":"pedro@correo.com","password":"clave-segura-123",
                                 "telefono":"3009876543","rol":"PRODUCTOR",
                                 "vereda":"La Concepción","descripcion":"Quesos de la finca El Roble"}
                                """))
                .andExpect(status().isCreated());
    }

    private Integer idPrimerPendiente(String token) throws Exception {
        String respuesta = mockMvc.perform(get(URL).param("estado", "PENDIENTE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$[0].id");
    }

    private String cuerpoEstado(String estado) {
        return """
                {"estado":"%s"}
                """.formatted(estado);
    }

    @Test
    void listarPendientes_comoAdmin_incluyeProductorRegistrado() throws Exception {
        registrarPedroProductor();

        mockMvc.perform(get(URL).param("estado", "PENDIENTE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("pedro@correo.com"))
                .andExpect(jsonPath("$[0].vereda").value("La Concepción"))
                .andExpect(jsonPath("$[0].estadoVerificacion").value("PENDIENTE"));
    }

    @Test
    void verificar_comoAdmin_cambiaEstadoYSaleDePendientes() throws Exception {
        registrarPedroProductor();
        String token = tokenAdmin();
        Integer id = idPrimerPendiente(token);

        mockMvc.perform(patch(URL + "/{id}/verificacion", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEstado("VERIFICADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVerificacion").value("VERIFICADO"));

        mockMvc.perform(get(URL).param("estado", "PENDIENTE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void verificar_productorYaRevisado_devuelve400() throws Exception {
        registrarPedroProductor();
        String token = tokenAdmin();
        Integer id = idPrimerPendiente(token);

        mockMvc.perform(patch(URL + "/{id}/verificacion", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEstado("RECHAZADO")))
                .andExpect(status().isOk());

        mockMvc.perform(patch(URL + "/{id}/verificacion", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEstado("VERIFICADO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verificar_conEstadoPendiente_devuelve400() throws Exception {
        registrarPedroProductor();
        String token = tokenAdmin();
        Integer id = idPrimerPendiente(token);

        mockMvc.perform(patch(URL + "/{id}/verificacion", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEstado("PENDIENTE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verificar_productorInexistente_devuelve404() throws Exception {
        mockMvc.perform(patch(URL + "/{id}/verificacion", 999_999)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoEstado("VERIFICADO")))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_comoComprador_devuelve403() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"María","email":"maria@correo.com","password":"clave-segura-123",
                                 "telefono":"3001234567","rol":"COMPRADOR"}
                                """))
                .andExpect(status().isCreated());

        String tokenComprador = login("maria@correo.com", "clave-segura-123");

        mockMvc.perform(get(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenComprador))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }
}