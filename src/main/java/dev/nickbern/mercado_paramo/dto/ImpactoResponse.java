package dev.nickbern.mercado_paramo.dto;

import java.math.BigDecimal;

public record ImpactoResponse(
        long productoresVerificados,
        int pedidosEntregados,
        BigDecimal totalVendido,
        BigDecimal recibidoPorProductores,
        BigDecimal comisionPlataforma,
        BigDecimal participacionPorcentaje
) {}
