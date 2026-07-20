package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Dto.ProveedorRequest;
import com.utp.RestoControl.Entity.Proveedor;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Repository.ProveedorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTests {

    @Mock
    private ProveedorRepository repository;

    @InjectMocks
    private ProveedorService service;

    @Test
    void registraProveedorActivoConDatosNormalizados() {
        ProveedorRequest request = request();
        when(repository.save(any(Proveedor.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Proveedor proveedor = service.crear(request);

        assertEquals("Distribuidora Central", proveedor.getRazonSocial());
        assertEquals("20123456789", proveedor.getRuc());
        assertEquals("ventas@central.pe", proveedor.getCorreo());
        assertEquals(true, proveedor.getActivo());
    }

    @Test
    void rechazaUnRucDuplicado() {
        ProveedorRequest request = request();
        when(repository.existsByRuc("20123456789")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.crear(request));
    }

    private ProveedorRequest request() {
        ProveedorRequest request = new ProveedorRequest();
        request.setRazonSocial("  Distribuidora Central  ");
        request.setRuc("20123456789");
        request.setCorreo("VENTAS@CENTRAL.PE");
        request.setActivo(true);
        return request;
    }
}
