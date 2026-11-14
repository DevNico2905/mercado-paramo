package dev.nickbern.mercado_paramo.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.pedidos.max-pendientes=2")
class LimitePedidosIntegrationTest extends IntegracionBase {

    private Integer stockDe(Integer productoId) throws Exception {
        String respuesta = mockMvc.perform(get("/api/productos/" + productoId))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.stock");
    }

    @Test
    void crear_superaElLimiteDePendientes_devuelve409YNoDescuentaStock() throws Exception {
        String tokenProductor = productorVerificado("Pedro", "pedro@correo.com");
        Integer productoId = crearProducto(tokenProductor, "Queso", "12000", 10);
        String tokenComprador = compradorMaria();

        crearPedido(tokenComprador, pedidoJson(item(productoId, 1)));
        crearPedido(tokenComprador, pedidoJson(item(productoId, 1)));

        mockMvc.perform(post("/api/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenComprador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson(item(productoId, 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("pedidos pendientes")));

        assertEquals(8, stockDe(productoId));
    }

    @Test
    void crear_trasCancelarUnPendiente_vuelveAPermitir() throws Exception {
        String tokenProductor = productorVerificado("Pedro", "pedro@correo.com");
        Integer productoId = crearProducto(tokenProductor, "Queso", "12000", 10);
        String tokenComprador = compradorMaria();

        Integer primero = crearPedido(tokenComprador, pedidoJson(item(productoId, 1)));
        crearPedido(tokenComprador, pedidoJson(item(productoId, 1)));

        mockMvc.perform(patch("/api/pedidos/" + primero + "/cancelar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenComprador)))
                .andExpect(status().isOk());

        crearPedido(tokenComprador, pedidoJson(item(productoId, 1)));
    }

    @Test
    void crear_pedidosAceptadosNoCuentanComoPendientes() throws Exception {
        String tokenProductor = productorVerificado("Pedro", "pedro@correo.com");
        Integer productoId = crearProducto(tokenProductor, "Queso", "12000", 10);
        String tokenComprador = compradorMaria();

        Integer primero = crearPedido(tokenComprador, pedidoJson(item(productoId, 1)));
        crearPedido(tokenComprador, pedidoJson(item(productoId, 1)));
        cambiarEstado(tokenProductor, primero, "ACEPTADO").andExpect(status().isOk());

        crearPedido(tokenComprador, pedidoJson(item(productoId, 1)));
    }
}