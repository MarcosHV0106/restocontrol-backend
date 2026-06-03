/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.RestoControl.Service;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.utp.RestoControl.Entity.Rol;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.RolRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RolService {
    
    private final RolRepository repository;
    
    @Transactional(readOnly = true)
    public List<Rol> listar() {
        return repository.findByEliminadoFalse();
    }

    @Transactional(readOnly = true)
    public Rol buscarPorId(Integer id) {
        return repository.findByIdRolAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado."));
    }

    @Transactional
    public Rol guardar(Rol rol) {
        validarRol(rol, null);
        rol.setIdRol(null);
        rol.setEliminado(false);

        return repository.save(rol);
    }

    @Transactional
    public Rol actualizar(Integer id, Rol datos) {
        Rol rol = buscarPorId(id);
        validarRol(datos, id);

        rol.setNombreRol(datos.getNombreRol());
        rol.setDescripcion(normalizarDescripcion(datos.getDescripcion()));

        return repository.save(rol);
    }

    @Transactional
    public void eliminar(Integer id) {
        Rol rol = buscarPorId(id);
        rol.setEliminado(true);
        repository.save(rol);
    }

    private void validarRol(Rol rol, Integer idActual) {
        Preconditions.checkArgument(rol != null, "El rol es obligatorio.");

        String nombre = normalizarObligatorio(rol.getNombreRol(), "El nombre del rol es obligatorio.");
        boolean nombreDuplicado = idActual == null
                ? repository.existsByNombreRolIgnoreCaseAndEliminadoFalse(nombre)
                : repository.existsByNombreRolIgnoreCaseAndIdRolNotAndEliminadoFalse(nombre, idActual);

        Preconditions.checkArgument(!nombreDuplicado, "Ya existe un rol activo con ese nombre.");

        rol.setNombreRol(nombre);
        rol.setDescripcion(normalizarDescripcion(rol.getDescripcion()));
    }

    private String normalizarObligatorio(String valor, String mensaje) {
        String normalizado = valor == null ? null : valor.trim();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(normalizado), mensaje);
        return normalizado;
    }

    private String normalizarDescripcion(String valor) {
        String normalizado = valor == null ? null : valor.trim();
        return MoreObjects.firstNonNull(normalizado, "");
    }
}
