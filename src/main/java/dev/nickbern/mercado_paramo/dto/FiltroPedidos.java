package dev.nickbern.mercado_paramo.dto;

import dev.nickbern.mercado_paramo.model.EstadoPedido;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record FiltroPedidos(
        EstadoPedido estado,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        OrdenPedido ordenarPor,
        Sort.Direction direccion
) {

    public LocalDateTime inicio() {
        return (desde != null ? desde : LocalDate.EPOCH).atStartOfDay();
    }

    public LocalDateTime fin() {
        return (hasta != null ? hasta : LocalDate.now()).atTime(LocalTime.MAX);
    }

    public Sort sort() {
        OrdenPedido campo = (ordenarPor != null) ? ordenarPor : OrdenPedido.FECHA;
        Sort.Direction sentido = (direccion != null) ? direccion : Sort.Direction.DESC;
        return Sort.by(sentido, campo.propiedad()).and(Sort.by(Sort.Direction.DESC, "id"));
    }

    public boolean rangoInvertido() {
        return desde != null && hasta != null && desde.isAfter(hasta);
    }
}