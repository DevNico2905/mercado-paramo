package dev.nickbern.mercado_paramo.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FiltrosPedidosIntegrationTest extends IntegracionBase {

    private String tokenPedro;
    private String tokenMaria;
    private Integer queso;
    private Integer pendiente;
    private Integer aceptado;
    private Integer rechazado;

    @BeforeEach
    void escenario() throws Exception {
        tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        queso = crearProducto(tokenPedro, "Queso campesino", "12000", 20);
        tokenMaria = compradorMaria();

        pendiente = crearPedido(tokenMaria, pedidoJson(item(queso, 1)));

        aceptado = crearPedido(tokenMaria, pedidoJson(item(queso, 3)));
        cambiarEstado(tokenPedro, aceptado, "ACEPTADO").andExpect(status().isOk());

        rechazado = crearPedido(tokenMaria, pedidoJson(item(queso, 2)));
        cambiarEstado(tokenPedro, rechazado, "RECHAZADO").andExpect(status().isOk());
    }

    /** GET con token y pares nombre/valor de query params. */
    private ResultActions listar(String url, String token, String... params) throws Exception {
        MockHttpServletRequestBuilder peticion = get(url)
                .header(HttpHeaders.AUTHORIZATION, bearer(token));
        for (int i = 0; i < params.length; i += 2) {
            peticion.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(peticion);
    }

    @Test
    void comprador_filtraPorEstado() throws Exception {
        listar("/api/pedidos", tokenMaria, "estado", "ACEPTADO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(aceptado));
    }

    @Test
    void ordenPorDefecto_esMasRecientePrimero() throws Exception {
        listar("/api/pedidos", tokenMaria)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(rechazado, aceptado, pendiente)));
    }

    @Test
    void productor_ordenaPorTotalAscendente() throws Exception {
        listar("/api/productor/pedidos", tokenPedro, "ordenarPor", "TOTAL", "direccion", "ASC")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(pendiente, rechazado, aceptado)));
    }

    @Test
    void filtroPorFecha_fueraDeRango_devuelveVacio() throws Exception {
        LocalDate manana = LocalDate.now().plusDays(1);

        listar("/api/pedidos", tokenMaria,
                "desde", manana.toString(), "hasta", manana.plusDays(1).toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rangoInvertido_devuelve400() throws Exception {
        listar("/api/pedidos", tokenMaria, "desde", "2026-12-01", "hasta", "2026-11-01")
                .andExpect(status().isBadRequest());
    }

    @Test
    void ordenarPorCampoNoPermitido_devuelve400() throws Exception {
        listar("/api/pedidos", tokenMaria, "ordenarPor", "PRECIO")
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_veLosPedidosDeTodosLosProductores() throws Exception {
        String tokenAna = productorVerificado("Ana", "ana@correo.com");
        Integer fresas = crearProducto(tokenAna, "Fresas", "8000", 10);
        crearPedido(tokenMaria, pedidoJson(item(fresas, 1)));

        listar("/api/admin/pedidos", tokenAdmin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        listar("/api/productor/pedidos", tokenPedro)
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void listadoAdmin_comoComprador_devuelve403() throws Exception {
        listar("/api/admin/pedidos", tokenMaria)
                .andExpect(status().isForbidden());
    }
}