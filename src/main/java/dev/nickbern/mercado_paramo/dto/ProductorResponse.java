package dev.nickbern.mercado_paramo.dto;

import dev.nickbern.mercado_paramo.model.EstadoVerificacion;
import dev.nickbern.mercado_paramo.model.Productor;
import dev.nickbern.mercado_paramo.model.Usuario;

public record ProductorResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        String vereda,
        String descripcion,
        EstadoVerificacion estadoVerificacion
) {
    public static ProductorResponse de(Productor productor) {
        Usuario usuario = productor.getUsuario();
        return new ProductorResponse(
                productor.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                productor.getVereda(),
                productor.getDescripcion(),
                productor.getEstadoVerificacion());
    }
}
