package dev.nickbern.mercado_paramo.exception;
import org.springframework.http.HttpStatus;

public class ReglaNegocioException extends NegocioException{

    public ReglaNegocioException(String mensaje, HttpStatus status) {
        super(mensaje, HttpStatus.BAD_REQUEST);
    }
}
