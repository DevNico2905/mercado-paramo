package dev.nickbern.mercado_paramo.integration;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Helpers compartidos por las pruebas de integración que recorren flujos completos de la API. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class IntegracionBase {

    protected static final String PASSWORD = "clave-segura-123";
    protected static final String ADMIN_EMAIL = "admin@mercado-paramo.local";
    protected static final String ADMIN_PASSWORD = "admin-test-123";

    @Autowired
    protected MockMvc mockMvc;

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String login(String email, String password) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    protected String tokenAdmin() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected void registrar(String nombre, String email, String rol) throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","email":"%s","password":"%s","telefono":"3001234567",
                                 "rol":"%s","vereda":"La Concepción"}
                                """.formatted(nombre, email, PASSWORD, rol)))
                .andExpect(status().isCreated());
    }

    protected String productorVerificado(String nombre, String email) throws Exception {
        registrar(nombre, email, "PRODUCTOR");
        String tokenAdmin = tokenAdmin();

        String pendientes = mockMvc.perform(get("/api/admin/productores").param("estado", "PENDIENTE")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andReturn().getResponse().getContentAsString();
        Integer idProductor = JsonPath.read(pendientes, "$[0].id");

        mockMvc.perform(patch("/api/admin/productores/{id}/verificacion", idProductor)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"VERIFICADO"}
                                """))
                .andExpect(status().isOk());

        return login(email, PASSWORD);
    }

    protected String compradorMaria() throws Exception {
        registrar("María", "maria@correo.com", "COMPRADOR");
        return login("maria@correo.com", PASSWORD);
    }
    
    protected Integer crearProducto(String tokenProductor, String nombre, String precio, int stock) throws Exception {
        String categorias = mockMvc.perform(get("/api/categorias"))
                .andReturn().getResponse().getContentAsString();
        Integer categoriaId = JsonPath.read(categorias, "$[0].id");

        String respuesta = mockMvc.perform(post("/api/productor/productos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenProductor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","unidad":"libra","precio":%s,"stock":%d,"categoriaId":%d}
                                """.formatted(nombre, precio, stock, categoriaId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    protected String item(Integer productoId, int cantidad) {
        return """
                {"productoId":%d,"cantidad":%d}""".formatted(productoId, cantidad);
    }

    protected String pedidoJson(String... items) {
        return """
                {"direccionEntrega":"Vereda La Concepción, finca El Roble","items":[%s]}
                """.formatted(String.join(",", items));
    }

    protected Integer crearPedido(String tokenComprador, String json) throws Exception {
        String respuesta = mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenComprador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    protected ResultActions cambiarEstado(String tokenProductor, Integer pedidoId, String estado) throws Exception {
        return mockMvc.perform(patch("/api/productor/pedidos/{id}/estado", pedidoId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenProductor))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"estado":"%s"}
                        """.formatted(estado)));
    }

    protected void entregar(String tokenProductor, Integer pedidoId) throws Exception {
        cambiarEstado(tokenProductor, pedidoId, "ACEPTADO").andExpect(status().isOk());
        cambiarEstado(tokenProductor, pedidoId, "ENTREGADO").andExpect(status().isOk());
    }

    /** Compara montos por valor (24000 == 24000.00), sin depender de cómo se serializa el número. */
    protected void assertMonto(String json, String ruta, String esperado) {
        Object valor = JsonPath.read(json, ruta);
        assertEquals(0, new BigDecimal(valor.toString()).compareTo(new BigDecimal(esperado)),
                ruta + " esperado " + esperado + " pero fue " + valor);
    }
}