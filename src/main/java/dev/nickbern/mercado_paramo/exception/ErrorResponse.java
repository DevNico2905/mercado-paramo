package dev.nickbern.mercado_paramo.exception;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> errores
) {
}
//
//   {
//           "timestamp": "2026-10-30T19:00:00",
//           "status": 409,
//           "error": "Conflict",
//           "message": "Stock insuficiente para: Queso campesino",
//           "path": "/api/pedidos",
//           "errores": []
//           }