package com.utp.RestoControl.Service;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.utp.RestoControl.Dto.AlimentoRequest;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.CategoriaAlimento;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.AlimentoRepository;
import com.utp.RestoControl.Repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlimentoService {

    private final AlimentoRepository repository;
    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<Alimento> listar() {
        return repository.findByEliminadoFalse();
    }

    @Transactional(readOnly = true)
    public List<Alimento> listarPorCategoria(Integer idCategoria) {
        buscarCategoriaActiva(idCategoria);
        return repository.findByCategoria_IdCategoriaAndEliminadoFalse(idCategoria);
    }

    @Transactional(readOnly = true)
    public Alimento buscarPorId(Integer id) {
        return repository.findByIdAlimentoAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alimento no encontrado."));
    }

    @Transactional
    public Alimento guardar(AlimentoRequest request) {
        validarRequest(request);

        Alimento alimento = new Alimento();
        alimento.setNombreAlimento(normalizarObligatorio(request.getNombreAlimento(), "El nombre del alimento es obligatorio."));
        alimento.setDescripcion(normalizarOpcional(request.getDescripcion()));
        alimento.setPrecio(request.getPrecio());
        alimento.setDisponible(MoreObjects.firstNonNull(request.getDisponible(), true));
        alimento.setStock(MoreObjects.firstNonNull(request.getStock(), 0));
        alimento.setCategoria(buscarCategoriaActiva(obtenerIdCategoria(request)));
        alimento.setEliminado(false);

        return repository.save(alimento);
    }

    @Transactional
    public Alimento actualizar(Integer id, AlimentoRequest request) {
        validarRequest(request);

        Alimento alimento = buscarPorId(id);
        alimento.setNombreAlimento(normalizarObligatorio(request.getNombreAlimento(), "El nombre del alimento es obligatorio."));
        alimento.setDescripcion(normalizarOpcional(request.getDescripcion()));
        alimento.setPrecio(request.getPrecio());
        alimento.setDisponible(MoreObjects.firstNonNull(request.getDisponible(), alimento.getDisponible()));
        alimento.setStock(MoreObjects.firstNonNull(request.getStock(), alimento.getStock()));
        alimento.setCategoria(buscarCategoriaActiva(obtenerIdCategoria(request)));

        return repository.save(alimento);
    }

    @Transactional
    public void eliminar(Integer id) {
        Alimento alimento = buscarPorId(id);
        alimento.setEliminado(true);
        repository.save(alimento);
    }

    private void validarRequest(AlimentoRequest request) {
        Preconditions.checkArgument(request != null, "El alimento es obligatorio.");
        normalizarObligatorio(request.getNombreAlimento(), "El nombre del alimento es obligatorio.");
        Preconditions.checkArgument(obtenerIdCategoria(request) != null, "La categoria del alimento es obligatoria.");
        Preconditions.checkArgument(request.getPrecio() != null && request.getPrecio().compareTo(BigDecimal.ZERO) > 0, "El precio debe ser mayor a cero.");
        Preconditions.checkArgument(request.getStock() == null || request.getStock() >= 0, "El stock no puede ser negativo.");
    }

    private Integer obtenerIdCategoria(AlimentoRequest request) {
        if (request.getIdCategoria() != null) {
            return request.getIdCategoria();
        }

        return request.getCategoria() == null ? null : request.getCategoria().getIdCategoria();
    }

    private CategoriaAlimento buscarCategoriaActiva(Integer idCategoria) {
        Preconditions.checkArgument(idCategoria != null, "La categoria es obligatoria.");
        return categoriaRepository.findByIdCategoriaAndEliminadoFalse(idCategoria)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada."));
    }

    private String normalizarObligatorio(String valor, String mensaje) {
        String normalizado = valor == null ? null : valor.trim();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(normalizado), mensaje);
        return normalizado;
    }

    private String normalizarOpcional(String valor) {
        return valor == null ? null : valor.trim();
    }
}
