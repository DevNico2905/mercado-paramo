package dev.nickbern.mercado_paramo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportesIntegrationTest extends IntegracionBase {

    private static final String URL_VENTAS = "/api/productor/reportes/ventas";
    private static final String URL_IMPACTO = "/api/admin/reportes/impacto";

    private String reporteVentas(String tokenProductor) throws Exception {
        return mockMvc.perform(get(URL_VENTAS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenProductor)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void ventas_sinPedidosEntregados_devuelveCerosYParticipacionNula() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");

        mockMvc.perform(get(URL_VENTAS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPedro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidosEntregados").value(0))
                .andExpect(jsonPath("$.participacionPorcentaje").value(nullValue()))
                .andExpect(jsonPath("$.productos.length()").value(0));
    }

    @Test
    void ventas_cuentaSoloPedidosEntregados() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer queso = crearProducto(tokenPedro, "Queso campesino", "12000", 10);
        String tokenMaria = compradorMaria();

        Integer entregado = crearPedido(tokenMaria, pedidoJson(item(queso, 2)));
        entregar(tokenPedro, entregado);

        crearPedido(tokenMaria, pedidoJson(item(queso, 1)));                 // queda PENDIENTE

        Integer rechazado = crearPedido(tokenMaria, pedidoJson(item(queso, 1)));
        cambiarEstado(tokenPedro, rechazado, "RECHAZADO");

        String reporte = reporteVentas(tokenPedro);

        mockMvc.perform(get(URL_VENTAS).header(HttpHeaders.AUTHORIZATION, bearer(tokenPedro)))
                .andExpect(jsonPath("$.pedidosEntregados").value(1))
                .andExpect(jsonPath("$.productos[0].cantidadVendida").value(2));

        assertMonto(reporte, "$.totalVendido", "24000");
        assertMonto(reporte, "$.comisionPlataforma", "1200");
        assertMonto(reporte, "$.ingresoNeto", "22800");
        assertMonto(reporte, "$.participacionPorcentaje", "95");
    }

    @Test
    void ventas_agrupaPorProductoYOrdenaPorTotal() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer queso = crearProducto(tokenPedro, "Queso campesino", "12000", 10);
        Integer fresas = crearProducto(tokenPedro, "Fresas", "8000", 10);
        String tokenMaria = compradorMaria();

        Integer pedido = crearPedido(tokenMaria, pedidoJson(item(queso, 1), item(fresas, 3)));
        entregar(tokenPedro, pedido);

        String reporte = reporteVentas(tokenPedro);

        mockMvc.perform(get(URL_VENTAS).header(HttpHeaders.AUTHORIZATION, bearer(tokenPedro)))
                .andExpect(jsonPath("$.productos.length()").value(2))
                .andExpect(jsonPath("$.productos[0].nombre").value("Fresas"))
                .andExpect(jsonPath("$.productos[1].nombre").value("Queso campesino"));

        assertMonto(reporte, "$.productos[0].totalVendido", "24000");
        assertMonto(reporte, "$.productos[1].totalVendido", "12000");
    }

    @Test
    void ventas_rangoInvertido_devuelve400() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");

        mockMvc.perform(get(URL_VENTAS)
                        .param("desde", "2026-12-01")
                        .param("hasta", "2026-11-01")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPedro)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ventas_comoComprador_devuelve403() throws Exception {
        String tokenMaria = compradorMaria();

        mockMvc.perform(get(URL_VENTAS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenMaria)))
                .andExpect(status().isForbidden());
    }

    @Test
    void impacto_comoAdmin_sumaTodosLosProductores() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer queso = crearProducto(tokenPedro, "Queso campesino", "12000", 10);
        String tokenAna = productorVerificado("Ana", "ana@correo.com");
        Integer fresas = crearProducto(tokenAna, "Fresas", "8000", 10);
        String tokenMaria = compradorMaria();

        entregar(tokenPedro, crearPedido(tokenMaria, pedidoJson(item(queso, 2))));   // 24 000
        entregar(tokenAna, crearPedido(tokenMaria, pedidoJson(item(fresas, 2))));    // 16 000

        String impacto = mockMvc.perform(get(URL_IMPACTO)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productoresVerificados").value(2))
                .andExpect(jsonPath("$.pedidosEntregados").value(2))
                .andReturn().getResponse().getContentAsString();

        assertMonto(impacto, "$.totalVendido", "40000");
        assertMonto(impacto, "$.recibidoPorProductores", "38000");
        assertMonto(impacto, "$.comisionPlataforma", "2000");
        assertMonto(impacto, "$.participacionPorcentaje", "95");
    }

    @Test
    void impacto_comoProductor_devuelve403() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");

        mockMvc.perform(get(URL_IMPACTO)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenPedro)))
                .andExpect(status().isForbidden());
    }
}