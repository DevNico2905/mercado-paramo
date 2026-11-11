package dev.nickbern.mercado_paramo.dto;
import dev.nickbern.mercado_paramo.model.Categoria;

public record CategoriaResponse(Long id, String nombre) {
    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre());
    }
}
