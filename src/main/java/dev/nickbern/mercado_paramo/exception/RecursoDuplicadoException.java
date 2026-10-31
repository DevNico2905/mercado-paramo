package dev.nickbern.mercado_paramo.exception;
import org.springframework.http.HttpStatus;

public class RecursoDuplicadoException extends NegocioException{

    public RecursoDuplicadoException(String mensaje) {
        super(mensaje, HttpStatus.CONFLICT);
    }
}
