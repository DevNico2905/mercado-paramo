package dev.nickbern.mercado_paramo.exception;
import org.springframework.http.HttpStatus;

public class TransicionInvalidaException extends NegocioException{

    public TransicionInvalidaException(String mensaje) {
        super(mensaje, HttpStatus.CONFLICT);
    }
}
