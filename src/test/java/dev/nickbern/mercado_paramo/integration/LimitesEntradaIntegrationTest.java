package dev.nickbern.mercado_paramo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LimitesEntradaIntegrationTest extends IntegracionBase {

    private String registroJson(String nombre, String password, String telefono, String descripcion) {
        return """
                {"nombre":"%s","email":"limites@correo.com","password":"%s","telefono":"%s",
                 "rol":"PRODUCTOR","vereda":"La Concepción","descripcion":"%s"}
                """.formatted(nombre, password, telefono, descripcion);
    }

    @Test
    void registro_nombreDeMasDe100Caracteres_devuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("a".repeat(101), PASSWORD, "3001234567", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    void registro_passwordDeMasDe72Caracteres_devuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Limites", "a".repeat(73), "3001234567", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    void registro_telefonoDeMasDe20Caracteres_devuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Limites", PASSWORD, "3".repeat(21), "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.telefono").exists());
    }

    @Test
    void registro_descripcionDeMasDe500Caracteres_devuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Limites", PASSWORD, "3001234567", "a".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").exists());
    }

    @Test
    void login_passwordDeMasDe72Caracteres_devuelve400yNo500() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(ADMIN_EMAIL, "a".repeat(200))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    void crearProducto_stockMayorA100000_devuelve400() throws Exception {
        String token = productorVerificado("Pedro", "pedro@correo.com");

        mockMvc.perform(post("/api/productor/productos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Queso","unidad":"libra","precio":12000,"stock":100001,"categoriaId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.stock").exists());
    }

    @Test
    void crearPedido_cantidadMayorA1000_devuelve400() throws Exception {
        String tokenProductor = productorVerificado("Pedro", "pedro@correo.com");
        Integer productoId = crearProducto(tokenProductor, "Queso", "12000", 5000);
        String tokenComprador = compradorMaria();

        mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenComprador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson(item(productoId, 1001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['items[0].cantidad']").exists());
    }

    @Test
    void crearPedido_conMasDe50Items_devuelve400() throws Exception {
        String tokenComprador = compradorMaria();
        String[] items = Collections.nCopies(51, item(1, 1)).toArray(String[]::new);

        mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenComprador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson(items)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.items").exists());
    }
}