package dev.nickbern.mercado_paramo.model;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoPedido {
    PENDIENTE, ACEPTADO, RECHAZADO, ENTREGADO, CANCELADO;

    /** Devuelve los estados a los que se puede pasar desde este estado. */
    public Set<EstadoPedido> transicionesPermitidas() {
        return switch (this) {
            case PENDIENTE -> EnumSet.of(ACEPTADO, RECHAZADO, CANCELADO);
            case ACEPTADO  -> EnumSet.of(ENTREGADO);
            case RECHAZADO, ENTREGADO, CANCELADO -> EnumSet.noneOf(EstadoPedido.class);
        };
    }

    /** Indica si se puede pasar de este estado a {@code nuevo}. */
    public boolean puedeCambiarA(EstadoPedido nuevo) {
        return transicionesPermitidas().contains(nuevo);
    }

    /** Indica si al llegar a este estado se debe devolver el stock (RN-05). */
    public boolean devuelveStock() {
        return this == RECHAZADO || this == CANCELADO;
    }
}