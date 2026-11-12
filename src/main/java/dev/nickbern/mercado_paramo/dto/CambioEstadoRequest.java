package dev.nickbern.mercado_paramo.dto;

import dev.nickbern.mercado_paramo.model.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPedido estado
) {}
