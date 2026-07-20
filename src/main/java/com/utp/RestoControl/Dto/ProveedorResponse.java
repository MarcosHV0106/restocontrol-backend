package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.Proveedor;
import java.time.LocalDateTime;

public record ProveedorResponse(
        Integer idProveedor,
        String razonSocial,
        String ruc,
        String contacto,
        String telefono,
        String correo,
        String direccion,
        Boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
    public static ProveedorResponse from(Proveedor proveedor) {
        return new ProveedorResponse(
                proveedor.getIdProveedor(), proveedor.getRazonSocial(), proveedor.getRuc(),
                proveedor.getContacto(), proveedor.getTelefono(), proveedor.getCorreo(),
                proveedor.getDireccion(), proveedor.getActivo(), proveedor.getFechaCreacion(),
                proveedor.getFechaActualizacion()
        );
    }
}
