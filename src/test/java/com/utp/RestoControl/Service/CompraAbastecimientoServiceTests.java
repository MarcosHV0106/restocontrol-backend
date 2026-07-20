package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Dto.CompraAbastecimientoRequest;
import com.utp.RestoControl.Dto.LoteInsumoRequest;
import com.utp.RestoControl.Entity.CompraAbastecimiento;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.LoteInsumo;
import com.utp.RestoControl.Entity.Proveedor;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.CompraAbastecimientoDetalleRepository;
import com.utp.RestoControl.Repository.CompraAbastecimientoRepository;
import com.utp.RestoControl.Repository.InsumoRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CompraAbastecimientoServiceTests {

    @Mock
    private CompraAbastecimientoRepository repository;
    @Mock
    private CompraAbastecimientoDetalleRepository detalleRepository;
    @Mock
    private ProveedorService proveedorService;
    @Mock
    private InsumoRepository insumoRepository;
    @Mock
    private LoteInsumoService loteService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AlertaInventarioService alertaService;

    @InjectMocks
    private CompraAbastecimientoService service;

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registraCompraCreaLoteActualizaCostoYSincronizaAlertas() {
        autenticar(12);
        Proveedor proveedor = new Proveedor();
        proveedor.setIdProveedor(3);
        proveedor.setActivo(true);
        Insumo insumo = new Insumo();
        insumo.setIdInsumo(8);
        insumo.setNombreInsumo("Arroz");
        insumo.setUnidadMedida("kg");
        insumo.setEliminado(false);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(12);
        LoteInsumo lote = new LoteInsumo();
        lote.setIdLote(90);
        lote.setCodigo("LOT-90");
        lote.setInsumo(insumo);
        lote.setFechaVencimiento(LocalDate.now().plusMonths(2));

        when(proveedorService.buscarActivo(3)).thenReturn(proveedor);
        when(usuarioRepository.findByIdUsuarioAndEliminadoFalse(12)).thenReturn(Optional.of(usuario));
        when(insumoRepository.findByIdInsumoAndEliminadoFalse(8)).thenReturn(Optional.of(insumo));
        when(loteService.crear(any(Integer.class), any(LoteInsumoRequest.class))).thenReturn(lote);
        when(repository.save(any(CompraAbastecimiento.class))).thenAnswer(invocacion -> {
            CompraAbastecimiento compra = invocacion.getArgument(0);
            compra.setIdCompra(40);
            return compra;
        });
        when(detalleRepository.saveAll(anyList())).thenAnswer(invocacion -> invocacion.getArgument(0));

        CompraAbastecimiento compra = service.registrar(new CompraAbastecimientoRequest(
                3, LocalDate.now(), "F001-25", "Compra semanal",
                List.of(new CompraAbastecimientoRequest.Detalle(
                        8, new BigDecimal("10"), new BigDecimal("4.50"), LocalDate.now().plusMonths(2)))));

        assertDecimal("45", compra.getTotal());
        assertEquals(1, compra.getDetalles().size());
        assertDecimal("4.50", insumo.getCostoUnitario());
        verify(loteService).crear(any(Integer.class), any(LoteInsumoRequest.class));
        verify(alertaService).sincronizar();
    }

    private void autenticar(Integer idUsuario) {
        UserPrincipal principal = org.mockito.Mockito.mock(UserPrincipal.class);
        when(principal.getId()).thenReturn(idUsuario);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ALMACENERO"))));
    }

    private void assertDecimal(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual));
    }
}
