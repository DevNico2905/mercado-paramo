package dev.nickbern.mercado_paramo.dto;

import dev.nickbern.mercado_paramo.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 254, message = "El email admite máximo 254 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "El telefono es obligatorio")
        @Size(max = 20, message = "El teléfono admite máximo 20 caracteres")
        String telefono,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        @Size(max = 100, message = "La vereda admite máximo 100 caracteres")
        String vereda,

        @Size(max = 500, message = "La descripción admite máximo 500 caracteres")
        String descripcion
) {
}
