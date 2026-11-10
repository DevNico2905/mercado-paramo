package dev.nickbern.mercado_paramo.dto;

import dev.nickbern.mercado_paramo.model.EstadoVerificacion;
import jakarta.validation.constraints.NotNull;

public record VerificacionRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoVerificacion estado
) {}
