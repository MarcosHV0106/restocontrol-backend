package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Dto.AuditoriaOpcionesResponse;
import com.utp.RestoControl.Dto.AuditoriaPaginaResponse;
import com.utp.RestoControl.Dto.AuditoriaRegistro;
import com.utp.RestoControl.Entity.AuditoriaOperacion;
import com.utp.RestoControl.Entity.Rol;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.AuditoriaOperacionRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AuditoriaOperacionServiceTests {

    @Mock
    private AuditoriaOperacionRepository repository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuditoriaOperacionService service;

    @Test
    void persisteLaOperacionConUnaInstantaneaLegibleDelUsuario() {
        Usuario usuario = usuarioAdministrador();
        when(usuarioRepository.findByIdUsuarioAndEliminadoFalse(7)).thenReturn(Optional.of(usuario));

        service.registrarOperacion(new AuditoriaRegistro(
                7, "admin@restocontrol.test", "ADMIN", "MENU", "ACTUALIZAR",
                "PUT", "/api/alimentos/15", "15", "EXITO", 200, 38,
                "127.0.0.1", "req-123", "PUT /api/alimentos/15 -> HTTP 200", null));

        ArgumentCaptor<AuditoriaOperacion> captor = ArgumentCaptor.forClass(AuditoriaOperacion.class);
        verify(repository).save(captor.capture());
        AuditoriaOperacion guardada = captor.getValue();
        assertEquals("María Administradora", guardada.getNombreUsuario());
        assertEquals("admin@restocontrol.test", guardada.getCorreoUsuario());
        assertEquals("MENU", guardada.getModulo());
        assertEquals("15", guardada.getRecursoId());
        assertEquals(38, guardada.getDuracionMs());
    }

    @Test
    void consultaUnaPaginaConTotalesDelRangoFiltrado() {
        AuditoriaOperacion operacion = operacionBase();
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<AuditoriaOperacion>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(operacion)));
        when(repository.count(org.mockito.ArgumentMatchers.<Specification<AuditoriaOperacion>>any())).thenReturn(8L, 2L);

        AuditoriaPaginaResponse response = service.consultar(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 20),
                null, "menu", null, null, "alimento", 0, 25);

        assertEquals(1, response.operaciones().size());
        assertEquals(8, response.exitosas());
        assertEquals(2, response.fallidas());
        assertEquals("ACTUALIZAR", response.operaciones().getFirst().accion());
    }

    @Test
    void rechazaRangosMayoresAUnAnio() {
        assertThrows(IllegalArgumentException.class, () -> service.consultar(
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 7, 20),
                null, null, null, null, null, 0, 25));
    }

    @Test
    void devuelveOpcionesDeFiltrosSinExponerEntidades() {
        when(repository.buscarModulos()).thenReturn(List.of("CAJA", "PEDIDOS"));
        when(repository.buscarAcciones()).thenReturn(List.of("REGISTRAR", "ACTUALIZAR"));
        when(repository.buscarUsuarios()).thenReturn(List.<Object[]>of(
                new Object[]{7, "María Administradora", "admin@restocontrol.test", "ADMIN"}));

        AuditoriaOpcionesResponse response = service.obtenerOpciones();

        assertEquals(List.of("CAJA", "PEDIDOS"), response.modulos());
        assertEquals(1, response.usuarios().size());
        assertEquals(7, response.usuarios().getFirst().idUsuario());
    }

    private AuditoriaOperacion operacionBase() {
        AuditoriaOperacion operacion = new AuditoriaOperacion();
        operacion.setIdAuditoria(1L);
        operacion.setFechaHora(LocalDateTime.of(2026, 7, 20, 10, 30));
        operacion.setIdUsuario(7);
        operacion.setCorreoUsuario("admin@restocontrol.test");
        operacion.setNombreUsuario("María Administradora");
        operacion.setRolUsuario("ADMIN");
        operacion.setModulo("MENU");
        operacion.setAccion("ACTUALIZAR");
        operacion.setMetodoHttp("PUT");
        operacion.setRuta("/api/alimentos/15");
        operacion.setRecursoId("15");
        operacion.setResultado("EXITO");
        operacion.setEstadoHttp(200);
        operacion.setDuracionMs(38L);
        return operacion;
    }

    private Usuario usuarioAdministrador() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setNombre("María");
        usuario.setApellido("Administradora");
        usuario.setCorreo("admin@restocontrol.test");
        usuario.setClave("clave");
        usuario.setRol(new Rol(1, "ADMIN", "Administración", false));
        usuario.setDisponible(true);
        usuario.setEliminado(false);
        return usuario;
    }
}
