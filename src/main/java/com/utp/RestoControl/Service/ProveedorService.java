package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.ProveedorRequest;
import com.utp.RestoControl.Entity.Proveedor;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.ProveedorRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository repository;

    @Transactional(readOnly = true)
    public List<Proveedor> listar() {
        return repository.findByEliminadoFalseOrderByRazonSocialAsc();
    }

    @Transactional(readOnly = true)
    public Proveedor buscarPorId(Integer idProveedor) {
        return repository.findByIdProveedorAndEliminadoFalse(idProveedor)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado."));
    }

    @Transactional(readOnly = true)
    public Proveedor buscarActivo(Integer idProveedor) {
        Proveedor proveedor = buscarPorId(idProveedor);
        if (!Boolean.TRUE.equals(proveedor.getActivo())) {
            throw new ConflictException("El proveedor seleccionado esta inactivo.");
        }
        return proveedor;
    }

    @Transactional
    public Proveedor crear(ProveedorRequest request) {
        DatosProveedor datos = validar(request);
        if (repository.existsByRuc(datos.ruc())) {
            throw new ConflictException("Ya existe un proveedor con ese RUC.");
        }

        Proveedor proveedor = new Proveedor();
        aplicar(proveedor, request, datos);
        proveedor.setActivo(request.getActivo() == null || request.getActivo());
        proveedor.setEliminado(false);
        return repository.save(proveedor);
    }

    @Transactional
    public Proveedor actualizar(Integer idProveedor, ProveedorRequest request) {
        Proveedor proveedor = buscarPorId(idProveedor);
        DatosProveedor datos = validar(request);
        if (repository.existsByRucAndIdProveedorNot(datos.ruc(), idProveedor)) {
            throw new ConflictException("Ya existe otro proveedor con ese RUC.");
        }
        aplicar(proveedor, request, datos);
        if (request.getActivo() != null) {
            proveedor.setActivo(request.getActivo());
        }
        return repository.save(proveedor);
    }

    @Transactional
    public void eliminar(Integer idProveedor) {
        Proveedor proveedor = buscarPorId(idProveedor);
        proveedor.setActivo(false);
        proveedor.setEliminado(true);
        repository.save(proveedor);
    }

    private void aplicar(Proveedor proveedor, ProveedorRequest request, DatosProveedor datos) {
        proveedor.setRazonSocial(datos.razonSocial());
        proveedor.setRuc(datos.ruc());
        proveedor.setContacto(normalizar(request.getContacto(), 120));
        proveedor.setTelefono(normalizarTelefono(request.getTelefono()));
        proveedor.setCorreo(normalizarCorreo(request.getCorreo()));
        proveedor.setDireccion(normalizar(request.getDireccion(), 250));
    }

    private DatosProveedor validar(ProveedorRequest request) {
        Preconditions.checkArgument(request != null, "Los datos del proveedor son obligatorios.");
        String razonSocial = normalizar(request.getRazonSocial(), 150);
        Preconditions.checkArgument(razonSocial != null, "La razon social es obligatoria.");
        String ruc = request.getRuc() == null ? "" : request.getRuc().replaceAll("\\s", "");
        Preconditions.checkArgument(ruc.matches("\\d{11}"), "El RUC debe contener 11 digitos.");
        return new DatosProveedor(razonSocial, ruc);
    }

    private String normalizarTelefono(String valor) {
        String telefono = normalizar(valor, 20);
        Preconditions.checkArgument(telefono == null || telefono.matches("[+0-9() -]{6,20}"),
                "El telefono no tiene un formato valido.");
        return telefono;
    }

    private String normalizarCorreo(String valor) {
        String correo = normalizar(valor, 120);
        Preconditions.checkArgument(correo == null || correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"),
                "El correo no tiene un formato valido.");
        return correo == null ? null : correo.toLowerCase(Locale.ROOT);
    }

    private String normalizar(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.trim();
        Preconditions.checkArgument(texto.length() <= maximo, "El texto supera la longitud permitida.");
        return texto;
    }

    private record DatosProveedor(String razonSocial, String ruc) {
    }
}
