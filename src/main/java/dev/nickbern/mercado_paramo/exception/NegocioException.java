package dev.nickbern.mercado_paramo.exception;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class NegocioException extends RuntimeException {

    private final HttpStatus status;

    protected NegocioException(String mensaje, HttpStatus status) {
        super(mensaje);
        this.status = status;
        // 1. ¿Cómo le pasas el mensaje a RuntimeException?
        // 2. ¿Qué haces con el status?
    }


}