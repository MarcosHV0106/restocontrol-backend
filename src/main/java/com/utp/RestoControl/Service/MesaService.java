package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.utp.RestoControl.Dto.MesaRequest;
import com.utp.RestoControl.Entity.EstadoMesa;
import com.utp.RestoControl.Entity.Mesa;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.MesaRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MesaService {

    private final MesaRepository repository;
    private final EstadoMesaService estadoMesaService;

    private static final Integer ESTADO_LIBRE = 1;
    private static final Integer ESTADO_OCUPADA = 2;
    private static final Integer ESTADO_RESERVADA = 3;
    private static final Integer ESTADO_COBRAR = 4;
    @Transactional(readOnly = true)
    public List<Mesa> listar() {
        return repository.findByEliminadoFalseOrderByNumeroMesaAsc();
    }

    @Transactional(readOnly = true)
    public Integer contarMesasLibres() {
        return repository.countByEstadoMesa_IdEstadoMesaAndEliminadoFalse(ESTADO_LIBRE);
    }

    @Transactional(readOnly = true)
    public Integer contarMesasOcupadas() {
        return repository.countByEstadoMesa_IdEstadoMesaAndEliminadoFalse(ESTADO_OCUPADA);
    }

    @Transactional(readOnly = true)
    public Integer contarMesasReservadas() {
        return repository.countByEstadoMesa_IdEstadoMesaAndEliminadoFalse(ESTADO_RESERVADA);
    }

    @Transactional(readOnly = true)
    public Integer contarMesasPorCobrar() {
        return repository.countByEstadoMesa_IdEstadoMesaAndEliminadoFalse(ESTADO_COBRAR);
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> resumen() {
        Map<String, Integer> resumen = new HashMap<>();
        resumen.put("libres", 0);
        resumen.put("ocupadas", 0);
        resumen.put("reservadas", 0);
        resumen.put("cobradas", 0);

        for (Object[] fila : repository.countMesasAgrupadasPorEstado()) {
            String estado = String.valueOf(fila[0]);
            Integer total = ((Number) fila[1]).intValue();

            switch (estado) {
                case "libre" -> resumen.put("libres", total);
                case "ocupada" -> resumen.put("ocupadas", total);
                case "reservada" -> resumen.put("reservadas", total);
                case "cobrar" -> resumen.put("cobradas", total);
                default -> {
                }
            }
        }

        return resumen;
    }

    @Transactional(readOnly = true)
    public Mesa buscarPorId(Integer id) {
        return repository.findByIdMesaAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada."));
    }

    @Transactional
    public Mesa guardar(MesaRequest request) {

        validarMesa(request, null);

        Mesa mesa = new Mesa();

        mesa.setNumeroMesa(
                request.getNumeroMesa()
        );

        mesa.setCapacidad(
                request.getCapacidad()
        );

        mesa.setPiso(
                request.getPiso()
        );

        EstadoMesa estadoLibre = estadoMesaService.buscarPorId(1);

        mesa.setEstadoMesa(estadoLibre);

        mesa.setEliminado(false);

        return repository.save(mesa);

    }

    @Transactional
    public Mesa actualizar(
            Integer id,
            MesaRequest request) {

        Mesa mesa = buscarPorId(id);

        validarMesa(request, id);

        mesa.setNumeroMesa(
                request.getNumeroMesa()
        );

        mesa.setCapacidad(
                request.getCapacidad()
        );

        mesa.setPiso(
                request.getPiso()
        );

        return repository.save(mesa);

    }

    @Transactional
    public void eliminar(Integer id) {
        Mesa mesa = buscarPorId(id);
        mesa.setEliminado(true);
        repository.save(mesa);
    }

    @Transactional
    public void actualizarEstado(Integer idMesa, Integer idEstadoMesa) {
        Mesa mesa = bloquearMesas(List.of(idMesa)).get(0);
        EstadoMesa estado = estadoMesaService.buscarPorId(idEstadoMesa);
        mesa.setEstadoMesa(estado);
        repository.save(mesa);
    }

    @Transactional
    public Mesa ocuparParaPedido(Integer idMesa) {
        Preconditions.checkArgument(idMesa != null, "La mesa es obligatoria para pedidos en salon.");
        Mesa mesa = bloquearMesas(List.of(idMesa)).get(0);
        Integer estadoActual = mesa.getEstadoMesa() == null
                ? null
                : mesa.getEstadoMesa().getIdEstadoMesa();
        Preconditions.checkState(
                ESTADO_LIBRE.equals(estadoActual) || ESTADO_RESERVADA.equals(estadoActual),
                "La mesa seleccionada ya no esta disponible."
        );
        mesa.setEstadoMesa(estadoMesaService.buscarPorId(ESTADO_OCUPADA));
        return repository.save(mesa);
    }

    @Transactional
    public Mesa transferirPedido(Integer idMesaOrigen, Integer idMesaDestino, boolean cuentaSolicitada) {
        Preconditions.checkArgument(idMesaOrigen != null, "La mesa actual es obligatoria.");
        Preconditions.checkArgument(idMesaDestino != null, "La mesa destino es obligatoria.");
        Preconditions.checkArgument(!Objects.equals(idMesaOrigen, idMesaDestino), "Selecciona una mesa diferente.");

        List<Mesa> mesas = bloquearMesas(List.of(idMesaOrigen, idMesaDestino));
        Preconditions.checkState(mesas.size() == 2, "No se encontro una de las mesas indicadas.");
        Mesa origen = mesas.stream()
                .filter(mesa -> idMesaOrigen.equals(mesa.getIdMesa()))
                .findFirst()
                .orElseThrow();
        Mesa destino = mesas.stream()
                .filter(mesa -> idMesaDestino.equals(mesa.getIdMesa()))
                .findFirst()
                .orElseThrow();

        Integer estadoDestino = destino.getEstadoMesa() == null
                ? null
                : destino.getEstadoMesa().getIdEstadoMesa();
        Preconditions.checkState(
                ESTADO_LIBRE.equals(estadoDestino) || ESTADO_RESERVADA.equals(estadoDestino),
                "La mesa destino ya no esta disponible."
        );

        origen.setEstadoMesa(estadoMesaService.buscarPorId(ESTADO_LIBRE));
        destino.setEstadoMesa(estadoMesaService.buscarPorId(
                cuentaSolicitada ? ESTADO_COBRAR : ESTADO_OCUPADA
        ));
        repository.saveAll(List.of(origen, destino));
        return destino;
    }

    @Transactional
    public void liberar(Integer idMesa) {
        if (idMesa == null) {
            return;
        }
        Mesa mesa = bloquearMesas(List.of(idMesa)).get(0);
        mesa.setEstadoMesa(estadoMesaService.buscarPorId(ESTADO_LIBRE));
        repository.save(mesa);
    }

    private List<Mesa> bloquearMesas(List<Integer> idsMesa) {
        List<Integer> ids = idsMesa.stream().filter(Objects::nonNull).distinct().sorted().toList();
        List<Mesa> mesas = repository.findActivasParaActualizar(ids);
        Preconditions.checkState(mesas.size() == ids.size(), "Mesa no encontrada.");
        return mesas;
    }

    private void validarMesa(
            MesaRequest mesa,
            Integer idActual) {

        Preconditions.checkArgument(
                mesa != null,
                "La mesa es obligatoria."
        );

        Preconditions.checkArgument(
                mesa.getNumeroMesa() != null
                && mesa.getNumeroMesa() > 0,
                "El numero de mesa debe ser mayor a cero."
        );

        Preconditions.checkArgument(
                mesa.getCapacidad() != null
                && mesa.getCapacidad() > 0,
                "La capacidad debe ser mayor a cero."
        );

        Preconditions.checkArgument(
                mesa.getPiso() != null
                && mesa.getPiso() > 0,
                "El piso debe ser mayor a cero."
        );

        boolean numeroDuplicado
                = idActual == null
                        ? repository.existsByNumeroMesaAndEliminadoFalse(
                                mesa.getNumeroMesa())
                        : repository.existsByNumeroMesaAndIdMesaNotAndEliminadoFalse(
                                mesa.getNumeroMesa(),
                                idActual);

        Preconditions.checkArgument(
                !numeroDuplicado,
                "Ya existe una mesa activa con ese numero."
        );

    }
}
