package com.utp.RestoControl.Logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.utp.RestoControl.Dto.AuditoriaRegistro;
import com.utp.RestoControl.Entity.Rol;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Security.UserPrincipal;
import com.utp.RestoControl.Service.AuditoriaOperacionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuditoriaOperacionInterceptorTests {

    @Mock
    private AuditoriaOperacionService service;

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registraUnaMutacionAutenticadaConModuloAccionYRecurso() throws Exception {
        autenticarAdministrador();
        AuditoriaOperacionInterceptor interceptor = new AuditoriaOperacionInterceptor(service);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "PUT", "/api/cocina/productos/41/disponibilidad");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        response.setHeader("X-Request-ID", "req-41");

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<AuditoriaRegistro> captor = ArgumentCaptor.forClass(AuditoriaRegistro.class);
        verify(service).registrarOperacion(captor.capture());
        AuditoriaRegistro registro = captor.getValue();
        assertEquals(7, registro.idUsuario());
        assertEquals("COCINA", registro.modulo());
        assertEquals("CAMBIAR_DISPONIBILIDAD", registro.accion());
        assertEquals("41", registro.recursoId());
        assertEquals("EXITO", registro.resultado());
        assertEquals("req-41", registro.requestId());
    }

    @Test
    void ignoraConsultasDeSoloLectura() throws Exception {
        AuditoriaOperacionInterceptor interceptor = new AuditoriaOperacionInterceptor(service);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/pedidos");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        verify(service, never()).registrarOperacion(any());
    }

    @Test
    void reconoceAlUsuarioAutenticadoDuranteUnLoginExitoso() throws Exception {
        AuditoriaOperacionInterceptor interceptor = new AuditoriaOperacionInterceptor(service);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.preHandle(request, response, new Object());
        autenticarAdministrador();
        interceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<AuditoriaRegistro> captor = ArgumentCaptor.forClass(AuditoriaRegistro.class);
        verify(service).registrarOperacion(captor.capture());
        assertEquals(7, captor.getValue().idUsuario());
        assertEquals("INICIAR_SESION", captor.getValue().accion());
    }

    @Test
    void unFalloDeLaBitacoraNoRompeLaOperacionPrincipal() throws Exception {
        AuditoriaOperacionInterceptor interceptor = new AuditoriaOperacionInterceptor(service);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/pedidos");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);
        doThrow(new IllegalStateException("base no disponible"))
                .when(service).registrarOperacion(any(AuditoriaRegistro.class));

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        verify(service).registrarOperacion(any(AuditoriaRegistro.class));
    }

    private void autenticarAdministrador() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setNombre("María");
        usuario.setApellido("Administradora");
        usuario.setCorreo("admin@restocontrol.test");
        usuario.setClave("clave");
        usuario.setRol(new Rol(1, "ADMIN", "Administración", false));
        UserPrincipal principal = new UserPrincipal(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
