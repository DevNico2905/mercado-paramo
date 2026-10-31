package dev.nickbern.mercado_paramo.exception;

import jakarta.validation.constraints.NotBlank;

public record DatoPrueba(@NotBlank String nombre) {}
