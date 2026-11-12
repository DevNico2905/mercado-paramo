package dev.nickbern.mercado_paramo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteVentasResponse(
        LocalDate desde,
        LocalDate hasta,
        int pedidosEntregados,
        BigDecimal totalVendido,
        BigDecimal comisionPlataforma,
        BigDecimal ingresoNeto,
        BigDecimal participacionPorcentaje,
        List<VentaProductoResponse> productos
) {}