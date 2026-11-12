package dev.nickbern.mercado_paramo.dto;

/** Campos por los que se permite ordenar los pedidos (lista blanca). */
public enum OrdenPedido {

    FECHA("fecha"),
    TOTAL("total"),
    ESTADO("estado");

    private final String propiedad;

    OrdenPedido(String propiedad) {
        this.propiedad = propiedad;
    }

    public String propiedad() {
        return propiedad;
    }
}