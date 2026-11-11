package dev.nickbern.mercado_paramo.service;

import dev.nickbern.mercado_paramo.dto.ProductoRequest;
import dev.nickbern.mercado_paramo.dto.ProductoResponse;
import dev.nickbern.mercado_paramo.exception.AccesoDenegadoException;
import dev.nickbern.mercado_paramo.exception.RecursoNoEncontradoException;
import dev.nickbern.mercado_paramo.model.Categoria;
import dev.nickbern.mercado_paramo.model.EstadoVerificacion;
import dev.nickbern.mercado_paramo.model.Producto;
import dev.nickbern.mercado_paramo.model.Productor;
import dev.nickbern.mercado_paramo.repository.CategoriaRepository;
import dev.nickbern.mercado_paramo.repository.ProductoRepository;
import dev.nickbern.mercado_paramo.repository.ProductorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductorRepository productorRepository;
    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<ProductoResponse> catalogo(Long categoriaId, Long productorId) {
        return productoRepository
                .buscarCatalogo(EstadoVerificacion.VERIFICADO, categoriaId, productorId)
                .stream()
                .map(ProductoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse detalle(Long id) {
        return productoRepository.findConDetallesById(id)
                .filter(Producto::isActivo)
                .map(ProductoResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> misProductos(String email) {
        Productor productor = productorDe(email);
        return productoRepository.findByProductorIdOrderByIdAsc(productor.getId()).stream()
                .map(ProductoResponse::de)
                .toList();
    }

    @Transactional
    public ProductoResponse crear(String email, ProductoRequest request) {
        Producto producto = new Producto();
        producto.setProductor(productorVerificadoDe(email));
        aplicarCambios(producto, request);
        return ProductoResponse.de(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(String email, Long id, ProductoRequest request) {
        Producto producto = productoPropio(email, id);
        aplicarCambios(producto, request);
        return ProductoResponse.de(producto);
    }

    @Transactional
    public void desactivar(String email, Long id) {
        productoPropio(email, id).setActivo(false);
    }

    private Productor productorDe(String email) {
        return productorRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Perfil de productor no encontrado"));
    }

    private Productor productorVerificadoDe(String email) {
        Productor productor = productorDe(email);
        if (productor.getEstadoVerificacion() != EstadoVerificacion.VERIFICADO) {
            throw new AccesoDenegadoException("Tu cuenta de productor aún no ha sido verificada");
        }
        return productor;
    }

    private Producto productoPropio(String email, Long id) {
        Productor productor = productorVerificadoDe(email);
        Producto producto = productoRepository.findConDetallesById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));

        if (!producto.getProductor().getId().equals(productor.getId())) {
            throw new AccesoDenegadoException("No puedes modificar productos de otro productor");
        }
        return producto;
    }

    private void aplicarCambios(Producto producto, ProductoRequest request) {
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));

        producto.setCategoria(categoria);
        producto.setNombre(request.nombre().strip());
        producto.setUnidad(request.unidad().strip());
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
    }
}