package dev.nickbern.mercado_paramo.service;

import dev.nickbern.mercado_paramo.dto.ImpactoResponse;
import dev.nickbern.mercado_paramo.dto.ReporteVentasResponse;
import dev.nickbern.mercado_paramo.dto.VentaProductoResponse;
import dev.nickbern.mercado_paramo.exception.RecursoNoEncontradoException;
import dev.nickbern.mercado_paramo.exception.ReglaNegocioException;
import dev.nickbern.mercado_paramo.model.*;
import dev.nickbern.mercado_paramo.repository.PedidoRepository;
import dev.nickbern.mercado_paramo.repository.ProductorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int DIAS_POR_DEFECTO = 30;

    private final PedidoRepository pedidoRepository;
    private final ProductorRepository productorRepository;

    @Transactional(readOnly = true)
    public ReporteVentasResponse ventasDelProductor(String emailProductor, LocalDate desde, LocalDate hasta) {
        LocalDate fin = (hasta != null) ? hasta : LocalDate.now();
        LocalDate inicio = (desde != null) ? desde : fin.minusDays(DIAS_POR_DEFECTO);

        if (inicio.isAfter(fin)) {
            throw new ReglaNegocioException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }

        Productor productor = productorRepository.findByUsuarioEmail(emailProductor)
                .orElseThrow(() -> new RecursoNoEncontradoException("Perfil de productor no encontrado"));

        List<Pedido> entregados = pedidoRepository.findByProductorIdAndEstadoAndFechaBetween(
                productor.getId(), EstadoPedido.ENTREGADO, inicio.atStartOfDay(), fin.atTime(LocalTime.MAX));

        BigDecimal total = sumar(entregados, Pedido::getTotal);
        BigDecimal comision = sumar(entregados, Pedido::getComision);
        BigDecimal neto = total.subtract(comision);

        return new ReporteVentasResponse(
                inicio, fin, entregados.size(),
                total, comision, neto, porcentaje(neto, total),
                ventasPorProducto(entregados));
    }

    @Transactional(readOnly = true)
    public ImpactoResponse impacto() {
        List<Pedido> entregados = pedidoRepository.findByEstado(EstadoPedido.ENTREGADO);

        BigDecimal total = sumar(entregados, Pedido::getTotal);
        BigDecimal comision = sumar(entregados, Pedido::getComision);
        BigDecimal neto = total.subtract(comision);

        return new ImpactoResponse(
                productorRepository.countByEstadoVerificacion(EstadoVerificacion.VERIFICADO),
                entregados.size(),
                total, neto, comision, porcentaje(neto, total));
    }

    private List<VentaProductoResponse> ventasPorProducto(List<Pedido> pedidos) {
        Map<Long, List<DetallePedido>> porProducto = pedidos.stream()
                .flatMap(pedido -> pedido.getDetalles().stream())
                .collect(Collectors.groupingBy(detalle -> detalle.getProducto().getId()));

        return porProducto.values().stream()
                .map(VentaProductoResponse::de)
                .sorted(Comparator.comparing(VentaProductoResponse::totalVendido).reversed())
                .toList();
    }

    private static BigDecimal sumar(List<Pedido> pedidos, Function<Pedido, BigDecimal> campo) {
        return pedidos.stream()
                .map(campo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return null;
        }
        return parte.multiply(CIEN).divide(total, 2, RoundingMode.HALF_UP);
    }
}
