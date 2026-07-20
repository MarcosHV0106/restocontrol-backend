package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Dto.EstimacionDiariaItemRequest;
import com.utp.RestoControl.Dto.EstimacionDiariaResponse;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.CategoriaAlimento;
import com.utp.RestoControl.Entity.EstimacionDiaria;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.RecetaAlimento;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Repository.AlimentoRepository;
import com.utp.RestoControl.Repository.DetallePedidoRepository;
import com.utp.RestoControl.Repository.EstimacionDiariaRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class EstimacionDiariaServiceTests {

    @Mock
    private EstimacionDiariaRepository estimacionRepository;
    @Mock
    private AlimentoRepository alimentoRepository;
    @Mock
    private DetallePedidoRepository detallePedidoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EstimacionDiariaService service;

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validaElConsumoAgregadoDeUnInsumoCompartido() {
        Insumo limon = insumo(1, "Limon", "kg", "10");
        Alimento ceviche = alimento(1, "Ceviche", limon, "6");
        Alimento limonada = alimento(2, "Limonada", limon, "6");
        when(alimentoRepository.findByEliminadoFalse()).thenReturn(List.of(ceviche, limonada));

        EstimacionDiariaResponse respuesta = service.validar(
                LocalDate.now(),
                List.of(
                        new EstimacionDiariaItemRequest(1, 1),
                        new EstimacionDiariaItemRequest(2, 1)
                )
        );

        assertFalse(respuesta.factible());
        assertTrue(respuesta.guardable());
        assertEquals(2, respuesta.totalPorciones());
        assertEquals(2, respuesta.platos().stream()
                .filter(plato -> "INSUFICIENTE".equals(plato.estado())).count());
        assertDecimal("12", respuesta.insumos().getFirst().cantidadRequerida());
        assertDecimal("2", respuesta.insumos().getFirst().faltante());
    }

    @Test
    @SuppressWarnings("unchecked")
    void guardaYReactivaUnaEstimacionFactibleConElUsuarioResponsable() {
        LocalDate fecha = LocalDate.now();
        Insumo pollo = insumo(1, "Pollo", "kg", "10");
        Alimento plato = alimento(1, "Pollo a la plancha", pollo, "2");
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        EstimacionDiaria existente = new EstimacionDiaria();
        existente.setIdEstimacionDiaria(20);
        existente.setFecha(fecha);
        existente.setAlimento(plato);
        existente.setPorciones(0);
        existente.setEliminado(true);

        when(alimentoRepository.findByEliminadoFalse()).thenReturn(List.of(plato));
        when(estimacionRepository.findByFecha(fecha)).thenReturn(List.of(existente));
        when(usuarioRepository.findByIdUsuarioAndEliminadoFalse(7)).thenReturn(Optional.of(usuario));
        autenticar(7, "ALMACENERO");

        EstimacionDiariaResponse respuesta = service.guardar(
                fecha,
                List.of(new EstimacionDiariaItemRequest(1, 3))
        );

        assertTrue(respuesta.factible());
        ArgumentCaptor<List<EstimacionDiaria>> cambios = ArgumentCaptor.forClass(List.class);
        verify(estimacionRepository).saveAll(cambios.capture());
        EstimacionDiaria guardada = cambios.getValue().getFirst();
        assertSame(existente, guardada);
        assertEquals(3, guardada.getPorciones());
        assertFalse(guardada.getEliminado());
        assertSame(usuario, guardada.getUsuario());
    }

    @Test
    void persisteLaDemandaAunqueRequieraAbastecimiento() {
        LocalDate fecha = LocalDate.now();
        Insumo pollo = insumo(1, "Pollo", "kg", "1");
        Alimento plato = alimento(1, "Pollo a la plancha", pollo, "2");
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setNombre("Maria");
        usuario.setApellido("Almacen");
        when(alimentoRepository.findByEliminadoFalse()).thenReturn(List.of(plato));
        when(estimacionRepository.findByFecha(fecha)).thenReturn(List.of());
        when(usuarioRepository.findByIdUsuarioAndEliminadoFalse(7)).thenReturn(Optional.of(usuario));
        autenticar(7, "ALMACENERO");

        EstimacionDiariaResponse respuesta = service.guardar(
                fecha,
                List.of(new EstimacionDiariaItemRequest(1, 1))
        );

        assertTrue(respuesta.guardada());
        assertTrue(respuesta.guardable());
        assertFalse(respuesta.factible());
        assertEquals("Maria Almacen", respuesta.responsable());
        verify(estimacionRepository).saveAll(anyList());
    }

    @Test
    void descuentaDelPlanLasPorcionesYaProcesadasPorCocina() {
        LocalDate fecha = LocalDate.now();
        Insumo pollo = insumo(1, "Pollo", "kg", "10");
        Alimento plato = alimento(1, "Pollo a la plancha", pollo, "2");
        DetallePedidoRepository.CantidadProcesadaPorAlimento consumo =
                mock(DetallePedidoRepository.CantidadProcesadaPorAlimento.class);
        when(consumo.getIdAlimento()).thenReturn(1);
        when(consumo.getCantidad()).thenReturn(2L);
        when(alimentoRepository.findByEliminadoFalse()).thenReturn(List.of(plato));
        when(detallePedidoRepository.sumarCantidadesProcesadas(any(), any()))
                .thenReturn(List.of(consumo));

        EstimacionDiariaResponse respuesta = service.validar(
                fecha,
                List.of(new EstimacionDiariaItemRequest(1, 5))
        );

        assertTrue(respuesta.factible());
        assertEquals(2, respuesta.porcionesProcesadas());
        assertEquals(3, respuesta.porcionesPendientes());
        assertDecimal("6", respuesta.insumos().getFirst().cantidadRequerida());
        assertEquals(2, respuesta.platos().getFirst().porcionesProcesadas());
        assertEquals(3, respuesta.platos().getFirst().porcionesPendientes());
    }

    @Test
    void impideGuardarUnPlatoHeredadoSinReceta() {
        Alimento legado = new Alimento();
        legado.setIdAlimento(1);
        legado.setNombreAlimento("Plato heredado");
        legado.setDisponible(true);
        legado.setEliminado(false);
        legado.setReceta(List.of());
        when(alimentoRepository.findByEliminadoFalse()).thenReturn(List.of(legado));

        ConflictException error = assertThrows(ConflictException.class, () -> service.guardar(
                LocalDate.now(),
                List.of(new EstimacionDiariaItemRequest(1, 10))
        ));

        assertTrue(error.getMessage().contains("sin receta"));
    }

    private Alimento alimento(
            Integer id,
            String nombre,
            Insumo insumo,
            String cantidad
    ) {
        CategoriaAlimento categoria = new CategoriaAlimento();
        categoria.setIdCategoria(1);
        categoria.setNombreCategoria("Fondos");
        categoria.setEliminado(false);

        Alimento alimento = new Alimento();
        alimento.setIdAlimento(id);
        alimento.setNombreAlimento(nombre);
        alimento.setCategoria(categoria);
        alimento.setDisponible(true);
        alimento.setEliminado(false);

        RecetaAlimento receta = new RecetaAlimento();
        receta.setAlimento(alimento);
        receta.setInsumo(insumo);
        receta.setCantidad(new BigDecimal(cantidad));
        alimento.setReceta(List.of(receta));
        return alimento;
    }

    private Insumo insumo(
            Integer id,
            String nombre,
            String unidad,
            String stock
    ) {
        Insumo insumo = new Insumo();
        insumo.setIdInsumo(id);
        insumo.setNombreInsumo(nombre);
        insumo.setUnidadMedida(unidad);
        insumo.setStockActual(new BigDecimal(stock));
        insumo.setEliminado(false);
        return insumo;
    }

    private void autenticar(Integer idUsuario, String rol) {
        UserPrincipal principal = org.mockito.Mockito.mock(UserPrincipal.class);
        when(principal.getId()).thenReturn(idUsuario);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void assertDecimal(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual));
    }
}
