
package com.utp.RestoControl.Service;

import com.utp.RestoControl.Dto.AlimentoInsumoRequest;
import com.utp.RestoControl.Dto.AlimentoInsumoResponse;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.AlimentoInsumo;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.AlimentoInsumoRepository;
import com.utp.RestoControl.Repository.AlimentoRepository;
import com.utp.RestoControl.Repository.InsumoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlimentoInsumoService {

    private final AlimentoInsumoRepository repository;
    private final AlimentoRepository alimentoRepository;
    private final InsumoRepository insumoRepository;

    private static final List<String> UNIDADES_VALIDAS
            = List.of("g", "kg", "ml", "lt", "unidad");

    public List<AlimentoInsumoResponse> listar() {

        return repository.findByEliminadoFalse()
                .stream()
                .map(this::convertirResponse)
                .toList();
    }

    public AlimentoInsumoResponse guardar(AlimentoInsumoRequest request) {

        if (request.getCantidadReferencial() == null
                || request.getCantidadReferencial() <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad referencial debe ser mayor a cero.");
        }

        if (request.getUnidadMedida() == null
                || !UNIDADES_VALIDAS.contains(request.getUnidadMedida().toLowerCase())) {

            throw new IllegalArgumentException("Unidad de medida no válida.");
        }

        if (repository.existsByAlimentoIdAlimentoAndInsumoIdInsumo(
                request.getIdAlimento(),
                request.getIdInsumo())) {

            throw new IllegalArgumentException(
                    "Este insumo ya está asociado al alimento.");
        }

        Alimento alimento = alimentoRepository
                .findByIdAlimentoAndEliminadoFalse(request.getIdAlimento())
                .orElseThrow(() -> new ResourceNotFoundException("Alimento no encontrado"));

        Insumo insumo = insumoRepository
                .findByIdInsumoAndEliminadoFalse(request.getIdInsumo())
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado"));

        AlimentoInsumo alimentoInsumo = new AlimentoInsumo();

        alimentoInsumo.setAlimento(alimento);
        alimentoInsumo.setInsumo(insumo);
        alimentoInsumo.setCantidadReferencial(request.getCantidadReferencial());
        alimentoInsumo.setUnidadMedida(request.getUnidadMedida());
        alimentoInsumo.setObservacion(request.getObservacion());

        AlimentoInsumo guardado = repository.save(alimentoInsumo);

        return convertirResponse(guardado);
    }

    public AlimentoInsumoResponse buscarPorId(Integer id) {

        AlimentoInsumo entidad = repository.findByIdAlimentoInsumoAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asociacion no encontrada"));

        return convertirResponse(entidad);
    }

    public void eliminar(Integer id) {

        AlimentoInsumo asociacion = repository.findByIdAlimentoInsumoAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asociacion no encontrada"));

        asociacion.setEliminado(true);

        repository.save(asociacion);
    }

    private AlimentoInsumoResponse convertirResponse(AlimentoInsumo entidad) {

        AlimentoInsumoResponse response = new AlimentoInsumoResponse();

        response.setIdAlimentoInsumo(entidad.getIdAlimentoInsumo());

        response.setIdAlimento(entidad.getAlimento().getIdAlimento());
        response.setNombreAlimento(entidad.getAlimento().getNombreAlimento());

        response.setIdInsumo(entidad.getInsumo().getIdInsumo());
        response.setNombreInsumo(entidad.getInsumo().getNombreInsumo());

        response.setCantidadReferencial(entidad.getCantidadReferencial());
        response.setUnidadMedida(entidad.getUnidadMedida());
        response.setObservacion(entidad.getObservacion());

        return response;
    }

    public List<AlimentoInsumoResponse> listarPorAlimento(Integer idAlimento) {

        return repository.findByAlimentoIdAlimentoAndEliminadoFalse(idAlimento)
                .stream()
                .map(this::convertirResponse)
                .toList();

    }

    public List<AlimentoInsumoResponse> listarPorInsumo(Integer idInsumo) {

        return repository.findByInsumoIdInsumoAndEliminadoFalse(idInsumo)
                .stream()
                .map(this::convertirResponse)
                .toList();

    }

}
