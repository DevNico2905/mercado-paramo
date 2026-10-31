package dev.nickbern.mercado_paramo.exception;
import org.springframework.http.HttpStatus;

public class StockInsuficienteException extends NegocioException{

    public StockInsuficienteException(String mensaje, HttpStatus status) {
        super(mensaje, HttpStatus.CONFLICT);
    }
}
