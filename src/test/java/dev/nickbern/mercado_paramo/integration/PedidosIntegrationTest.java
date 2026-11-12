package dev.nickbern.mercado_paramo.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PedidosIntegrationTest {

    private static final String PASSWORD = "clave-segura-123";

    @Autowired
    private MockMvc mockMvc;

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String login(String email) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, email.startsWith("admin") ? "admin-test-123" : PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private void registrar(String nombre, String email, String rol) throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","email":"%s","password":"%s","telefono":"3001234567",
                                 "rol":"%s","vereda":"La Concepción"}
                                """.formatted(nombre, email, PASSWORD, rol)))
                .andExpect(status().isCreated());
    }

    private String productorVerificado(String nombre, String email) throws Exception {
        registrar(nombre, email, "PRODUCTOR");
        String tokenAdmin = login("admin@mercado-paramo.local");

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

        return login(email);
    }

    private String compradorMaria() throws Exception {
        registrar("María", "maria@correo.com", "COMPRADOR");
        return login("maria@correo.com");
    }

    private Integer crearProducto(String tokenProductor, String nombre, String precio, int stock) throws Exception {
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

    private String item(Integer productoId, int cantidad) {
        return """
                {"productoId":%d,"cantidad":%d}""".formatted(productoId, cantidad);
    }

    private String pedidoJson(String... items) {
        return """
                {"direccionEntrega":"Vereda La Concepción, finca El Roble","items":[%s]}
                """.formatted(String.join(",", items));
    }

    private ResultActions postPedido(String tokenComprador, String json) throws Exception {
        return mockMvc.perform(post("/api/pedidos")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenComprador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private Integer crearPedido(String tokenComprador, String json) throws Exception {
        String respuesta = postPedido(tokenComprador, json)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    private ResultActions cambiarEstado(String tokenProductor, Integer pedidoId, String estado) throws Exception {
        return mockMvc.perform(patch("/api/productor/pedidos/{id}/estado", pedidoId)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenProductor))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"estado":"%s"}
                        """.formatted(estado)));
    }

    private int stockDe(Integer productoId) throws Exception {
        String respuesta = mockMvc.perform(get("/api/productos/{id}", productoId))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.stock");
    }

    /** Compara montos por valor (24000 == 24000.00), sin depender de cómo se serializa el número. */
    private void assertMonto(String json, String ruta, String esperado) {
        Object valor = JsonPath.read(json, ruta);
        assertEquals(0, new BigDecimal(valor.toString()).compareTo(new BigDecimal(esperado)),
                ruta + " esperado " + esperado + " pero fue " + valor);
    }

    @Test
    void crear_pedidoValido_calculaTotalesYDescuentaStock() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer queso = crearProducto(tokenPedro, "Queso campesino", "12000", 10);
        String tokenMaria = compradorMaria();

        String respuesta = postPedido(tokenMaria, pedidoJson(item(queso, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.productorNombre").value("Pedro"))
                .andExpect(jsonPath("$.detalles.length()").value(1))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(2))
                .andReturn().getResponse().getContentAsString();

        assertMonto(respuesta, "$.total", "24000");
        assertMonto(respuesta, "$.comision", "1200");
        assertMonto(respuesta, "$.montoProductor", "22800");
        assertEquals(8, stockDe(queso));
    }

    @Test
    void crear_sinStockSuficiente_devuelve409YNoDescuenta() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer queso = crearProducto(tokenPedro, "Queso campesino", "12000", 10);
        String tokenMaria = compradorMaria();

        postPedido(tokenMaria, pedidoJson(item(queso, 11)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Stock insuficiente para: Queso campesino (disponible: 10)"));

        assertEquals(10, stockDe(queso));
    }

    @Test
    void crear_productosDeDosProductores_devuelve400() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer queso = crearProducto(tokenPedro, "Queso campesino", "12000", 10);
        String tokenAna = productorVerificado("Ana", "ana@correo.com");
        Integer fresas = crearProducto(tokenAna, "Fresas", "8000", 10);
        String tokenMaria = compradorMaria();

        postPedido(tokenMaria, pedidoJson(item(queso, 1), item(fresas, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Un pedido solo puede incluir productos de un mismo productor"));
    }

    @Test
    void crear_productoRepetido_devuelve400() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer queso = crearProducto(tokenPedro, "Queso campesino", "12000", 10);
        String tokenMaria = compradorMaria();

        postPedido(tokenMaria, pedidoJson(item(queso, 1), item(queso, 2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_sinItems_devuelve400() throws Exception {
        String tokenMaria = compradorMaria();

        postPedido(tokenMaria, pedidoJson())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.items").exists());
    }

    @Test
    void crear_comoProductor_devuelve403() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer queso = crearProducto(tokenPedro, "Queso campesino", "12000", 10);

        postPedido(tokenPedro, pedidoJson(item(queso, 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void flujoCompleto_pendienteAceptadoEntregado() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer queso = crearProducto(tokenPedro, "Queso campesino", "12000", 10);
        String tokenMaria = compradorMaria();
        Integer pedido = crearPedido(tokenMaria, pedidoJson(item(queso, 2)));

        mockMvc.perform(get("/api/productor/pedidos").param("estado", "PENDIENTE")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPedro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        cambiarEstado(tokenPedro, pedido, "ACEPTADO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADO"));

        cambiarEstado(tokenPedro, pedido, "ENTREGADO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));

        mockMvc.perform(get("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenMaria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("ENTREGADO"));
    }

    @Test
    void rechazar_devuelveElStock() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer queso = crearProducto(tokenPedro, "Queso campesino", "12000", 10);
        String tokenMaria = compradorMaria();
        Integer pedido = crearPedido(tokenMaria, pedidoJson(item(queso, 3)));
        assertEquals(7, stockDe(queso));

        cambiarEstado(tokenPedro, pedido, "RECHAZADO")
                .andExpect(status().isOk());

        assertEquals(10, stockDe(queso));
    }

    @Test
    void cancelar_pedidoPendiente_devuelveElStock() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer queso = crearProducto(tokenPedro, "Queso campesino", "12000", 10);
        String tokenMaria = compradorMaria();
        Integer pedido = crearPedido(tokenMaria, pedidoJson(item(queso, 4)));

        mockMvc.perform(patch("/api/pedidos/{id}/cancelar", pedido)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenMaria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        assertEquals(10, stockDe(queso));
    }

    @Test
    void cancelar_pedidoAceptado_devuelve409() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer queso = crearProducto(tokenPedro, "Queso campesino", "12000", 10);
        String tokenMaria = compradorMaria();
        Integer pedido = crearPedido(tokenMaria, pedidoJson(item(queso, 1)));
        cambiarEstado(tokenPedro, pedido, "ACEPTADO").andExpect(status().isOk());

        mockMvc.perform(patch("/api/pedidos/{id}/cancelar", pedido)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenMaria)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede pasar un pedido de ACEPTADO a CANCELADO"));
    }

    @Test
    void productor_noPuedeCancelar_devuelve400() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer queso = crearProducto(tokenPedro, "Queso campesino", "12000", 10);
        String tokenMaria = compradorMaria();
        Integer pedido = crearPedido(tokenMaria, pedidoJson(item(queso, 1)));

        cambiarEstado(tokenPedro, pedido, "CANCELADO")
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarEstado_pedidoDeOtroProductor_devuelve403() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer queso = crearProducto(tokenPedro, "Queso campesino", "12000", 10);
        String tokenMaria = compradorMaria();
        Integer pedido = crearPedido(tokenMaria, pedidoJson(item(queso, 1)));

        String tokenAna = productorVerificado("Ana", "ana@correo.com");

        cambiarEstado(tokenAna, pedido, "ACEPTADO")
                .andExpect(status().isForbidden());
    }
}