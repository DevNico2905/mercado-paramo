package dev.nickbern.mercado_paramo.exception;

import org.springframework.http.HttpStatus;

public class LimitePedidosException extends NegocioException {
    public LimitePedidosException(String mensaje) {
        super(mensaje, HttpStatus.CONFLICT);
    }
}