package com.utp.RestoControl.Service;

import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.AlimentoRepository;
import com.utp.RestoControl.Repository.PedidoRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockAlimentoService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final AlimentoRepository alimentoRepository;
    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public void validarDisponibilidadParaPedido(Pedido pedido) {
        Map<Integer, Integer> cantidades = cantidadesPorAlimento(pedido);
        if (cantidades.isEmpty()) {
            throw new ConflictException("El pedido no contiene productos.");
        }

        for (Map.Entry<Integer, Integer> item : cantidades.entrySet()) {
            Alimento alimento = alimentoRepository.findByIdAlimentoAndEliminadoFalse(item.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado."));
            validarProducto(alimento, item.getValue());
        }
    }

    @Transactional
    public void descontarParaPedido(Pedido pedido) {
        if (pedido.getFechaDescuentoStock() != null) {
            return;
        }

        Map<Integer, Integer> cantidades = cantidadesPorAlimento(pedido);
        if (cantidades.isEmpty()) {
            throw new ConflictException("El pedido no contiene productos.");
        }

        for (Integer idAlimento : cantidades.keySet().stream().sorted().toList()) {
            Alimento alimento = alimentoRepository.findActivoParaActualizarStock(idAlimento)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado."));
            int cantidad = cantidades.get(idAlimento);
            validarProducto(alimento, cantidad);
            alimento.setStock(alimento.getStock() - cantidad);
            alimentoRepository.save(alimento);
        }

        pedido.setFechaDescuentoStock(LocalDateTime.now(ZONA_LIMA));
        pedidoRepository.save(pedido);
    }

    private void validarProducto(Alimento alimento, int cantidadSolicitada) {
        if (!Boolean.TRUE.equals(alimento.getDisponible())) {
            throw new ConflictException(alimento.getNombreAlimento() + " esta deshabilitado en el menu.");
        }
        if (Boolean.TRUE.equals(alimento.getBloqueadoCocina())) {
            throw new ConflictException(alimento.getNombreAlimento() + " no esta disponible temporalmente.");
        }
        int stock = Math.max(0, alimento.getStock() == null ? 0 : alimento.getStock());
        if (stock < cantidadSolicitada) {
            throw new ConflictException("Stock insuficiente de " + alimento.getNombreAlimento()
                    + ". Disponible: " + stock + ", solicitado: " + cantidadSolicitada + ".");
        }
    }

    private Map<Integer, Integer> cantidadesPorAlimento(Pedido pedido) {
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        List<DetallePedido> detalles = pedido.getDetalles() == null ? List.of() : pedido.getDetalles();
        detalles.stream()
                .filter(detalle -> !Boolean.TRUE.equals(detalle.getEliminado()))
                .filter(detalle -> detalle.getIdAlimento() != null)
                .filter(detalle -> detalle.getCantidad() != null && detalle.getCantidad() > 0)
                .forEach(detalle -> cantidades.merge(
                        detalle.getIdAlimento().getIdAlimento(),
                        detalle.getCantidad(),
                        Integer::sum
                ));
        return cantidades;
    }
}
