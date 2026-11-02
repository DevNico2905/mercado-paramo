package dev.nickbern.mercado_paramo.dto;
import dev.nickbern.mercado_paramo.model.EstadoVerificacion;
import dev.nickbern.mercado_paramo.model.Productor;
import dev.nickbern.mercado_paramo.model.Rol;
import dev.nickbern.mercado_paramo.model.Usuario;
import lombok.Builder;
import java.time.LocalDateTime;

public record RegistroResponse(
        Long id,
        String nombre,
        String email,
        Rol rol,
        EstadoVerificacion estadoVerificacion,
        LocalDateTime fechaRegistro
) {
    public static RegistroResponse de(Usuario usuario, Productor productor) {
        return new RegistroResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                productor != null ? productor.getEstadoVerificacion() : null,
                usuario.getFechaRegistro()
        );
    }
}
