package dev.nickbern.mercado_paramo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PedidoRequest(
        @NotBlank(message = "La dirección de entrega es obligatoria")
        @Size(max = 200, message = "La dirección admite máximo 200 caracteres")
        String direccionEntrega,

        @NotEmpty(message = "El pedido debe tener al menos un producto")
        @Valid
        List<ItemPedidoRequest> items
) {}
