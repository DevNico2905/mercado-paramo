package dev.nickbern.mercado_paramo.exception;
import org.springframework.http.HttpStatus;

public class RecursoNoEncontradoException extends NegocioException{

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje, HttpStatus.NOT_FOUND);
    }
}
