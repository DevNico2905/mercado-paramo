package dev.nickbern.mercado_paramo.dto;

public record LoginResponse(String token, String tipo, long expiraEnSegundos) {}
