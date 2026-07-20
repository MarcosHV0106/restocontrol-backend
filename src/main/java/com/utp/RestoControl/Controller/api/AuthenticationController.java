package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.ActivacionCuentaRequest;
import com.utp.RestoControl.Dto.LoginRequest;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.UsuarioRepository;
import com.utp.RestoControl.Service.UsuarioService;
import com.utp.RestoControl.Security.JwtUtil; // IMPORTANTE: Asegúrate de que la ruta coincida con tu paquete

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    // Inyectamos nuestra nueva clase utilitaria de JWT
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 1. Autenticamos al usuario usando el AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getCorreo(),
                            loginRequest.getClave()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 2. Extraemos el UserDetails autenticado
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // 3. Generamos el Token JWT
            String jwtToken = jwtUtil.generateToken(userDetails);

            // 4. Buscamos el usuario en la BD para devolver sus datos al Frontend
            Usuario usuario = usuarioRepository.findByCorreoIgnoreCaseAndEliminadoFalse(loginRequest.getCorreo())
                    .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

            // 5. Armamos la respuesta JSON
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login exitoso");

            // ¡ENVIAMOS EL TOKEN!
            response.put("token", jwtToken);

            response.put("usuario", Map.of(
                    "idUsuario", usuario.getIdUsuario(),
                    "nombre", usuario.getNombre(),
                    "apellido", usuario.getApellido(),
                    "correo", usuario.getCorreo(),
                    "rol", usuario.getRol() != null
                            ? usuario.getRol().getNombreRol()
                            : "SIN_ROL"
            ));

            LOGGER.info(
                    "Autenticacion resultado=EXITO usuarioId={} rol={}",
                    usuario.getIdUsuario(),
                    usuario.getRol() == null ? "SIN_ROL" : usuario.getRol().getNombreRol()
            );

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            LOGGER.warn("Autenticacion resultado=FALLO tipo=BAD_CREDENTIALS");
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Correo o contraseña incorrectos");
            return ResponseEntity.badRequest().body(response);

        } catch (AuthenticationException e) {
            LOGGER.warn("Autenticacion resultado=FALLO tipo={}", e.getClass().getSimpleName());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "No se pudo iniciar sesion");
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            LOGGER.error("Autenticacion resultado=ERROR tipo={}", e.getClass().getSimpleName(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ocurrio un error interno al iniciar sesion");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/activaciones/{token}")
    public ResponseEntity<?> validarActivacion(@PathVariable String token) {
        return ResponseEntity.ok(usuarioService.validarTokenActivacion(token));
    }

    @PostMapping("/activaciones/{token}/crear-clave")
    public ResponseEntity<?> crearClaveActivacion(
            @PathVariable String token,
            @RequestBody ActivacionCuentaRequest request) {
        return ResponseEntity.ok(usuarioService.crearClaveActivacion(token, request));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // En un esquema JWT (Stateless), el backend no guarda estado.
        // El verdadero "logout" consiste en que el Frontend (Vue) elimine el token del localStorage.
        // Mantenemos la limpieza del contexto de seguridad por buena práctica para el hilo actual.
        SecurityContextHolder.clearContext();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logout exitoso (Recuerde eliminar el token en el cliente)");
        LOGGER.info("Cierre de sesion resultado=EXITO");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify() {
        // Esto funcionará perfecto porque el JwtAuthenticationFilter rellena el contexto
        // antes de que la petición llegue aquí.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            response.put("authenticated", true);
            response.put("username", authentication.getName());
            response.put("authorities", authentication.getAuthorities());
        } else {
            response.put("authenticated", false);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/cambiar-contrasena")
    public ResponseEntity<?> cambiarContrasena(@RequestBody Map<String, String> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.badRequest().body(response);
            }

            String correo = authentication.getName();
            String claveActual = request.get("claveActual");
            String claveNueva = request.get("claveNueva");

            if (claveActual == null || claveActual.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "La contraseña actual es requerida");
                return ResponseEntity.badRequest().body(response);
            }

            if (claveNueva == null || claveNueva.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "La contraseña nueva es requerida");
                return ResponseEntity.badRequest().body(response);
            }

            Usuario usuario = usuarioRepository.findByCorreoIgnoreCaseAndEliminadoFalse(correo)
                    .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

            usuarioService.cambiarContrasena(usuario.getIdUsuario(), claveActual, claveNueva);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Contraseña actualizada correctamente");
            LOGGER.info("Cambio de contrasena resultado=EXITO usuarioId={}", usuario.getIdUsuario());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Cambio de contrasena resultado=FALLO tipo={}", e.getClass().getSimpleName());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            LOGGER.error("Cambio de contrasena resultado=ERROR tipo={}", e.getClass().getSimpleName(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ocurrio un error interno al cambiar la contrasena");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
