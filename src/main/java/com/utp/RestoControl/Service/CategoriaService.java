package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.utp.RestoControl.Entity.CategoriaAlimento;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.CategoriaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repository;

    @Transactional(readOnly = true)
    public List<CategoriaAlimento> listar() {
        return repository.findByEliminadoFalse();
    }

    @Transactional(readOnly = true)
    public CategoriaAlimento buscarPorId(Integer id) {
        return repository.findByIdCategoriaAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada."));
    }

    @Transactional
    public CategoriaAlimento guardar(CategoriaAlimento categoria) {
        validarCategoria(categoria, null);
        categoria.setIdCategoria(null);
        categoria.setEliminado(false);

        return repository.save(categoria);
    }

    @Transactional
    public CategoriaAlimento actualizar(Integer id, CategoriaAlimento datos) {
        CategoriaAlimento categoria = buscarPorId(id);
        validarCategoria(datos, id);

        categoria.setNombreCategoria(datos.getNombreCategoria());
        categoria.setDescripcion(normalizarOpcional(datos.getDescripcion()));

        return repository.save(categoria);
    }

    @Transactional
    public void eliminar(Integer id) {
        CategoriaAlimento categoria = buscarPorId(id);
        categoria.setEliminado(true);
        repository.save(categoria);
    }

    private void validarCategoria(CategoriaAlimento categoria, Integer idActual) {
        Preconditions.checkArgument(categoria != null, "La categoria es obligatoria.");

        String nombre = normalizarObligatorio(categoria.getNombreCategoria(), "El nombre de la categoria es obligatorio.");
        boolean nombreDuplicado = idActual == null
                ? repository.existsByNombreCategoriaIgnoreCaseAndEliminadoFalse(nombre)
                : repository.existsByNombreCategoriaIgnoreCaseAndIdCategoriaNotAndEliminadoFalse(nombre, idActual);

        Preconditions.checkArgument(!nombreDuplicado, "Ya existe una categoria activa con ese nombre.");

        categoria.setNombreCategoria(nombre);
        categoria.setDescripcion(normalizarOpcional(categoria.getDescripcion()));
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
