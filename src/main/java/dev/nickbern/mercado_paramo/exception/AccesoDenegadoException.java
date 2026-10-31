package dev.nickbern.mercado_paramo.exception;
import org.springframework.http.HttpStatus;

public class AccesoDenegadoException extends NegocioException{

    public AccesoDenegadoException(String mensaje) {
        super(mensaje, HttpStatus.FORBIDDEN);
    }
}
