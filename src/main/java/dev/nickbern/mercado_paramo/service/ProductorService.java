package dev.nickbern.mercado_paramo.service;

import dev.nickbern.mercado_paramo.dto.ProductorResponse;
import dev.nickbern.mercado_paramo.exception.RecursoNoEncontradoException;
import dev.nickbern.mercado_paramo.exception.ReglaNegocioException;
import dev.nickbern.mercado_paramo.model.EstadoVerificacion;
import dev.nickbern.mercado_paramo.model.Productor;
import dev.nickbern.mercado_paramo.repository.ProductorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductorService {

    private final ProductorRepository productorRepository;

    @Transactional(readOnly = true)
    public List<ProductorResponse> listar(EstadoVerificacion estado) {
        List<Productor> productores = (estado == null)
                ? productorRepository.findAllByOrderByIdAsc()
                : productorRepository.findByEstadoVerificacionOrderByIdAsc(estado);

        return productores.stream()
                .map(ProductorResponse::de)
                .toList();
    }

    @Transactional
    public ProductorResponse actualizarVerificacion(Long id, EstadoVerificacion nuevoEstado) {
        if (nuevoEstado == EstadoVerificacion.PENDIENTE) {
            throw new ReglaNegocioException("El estado debe ser VERIFICADO o RECHAZADO");
        }

        Productor productor = productorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Productor no encontrado"));

        if (productor.getEstadoVerificacion() != EstadoVerificacion.PENDIENTE) {
            throw new ReglaNegocioException(
                    "El productor ya fue revisado: " + productor.getEstadoVerificacion());
        }

        productor.setEstadoVerificacion(nuevoEstado);
        return ProductorResponse.de(productor);
    }
}