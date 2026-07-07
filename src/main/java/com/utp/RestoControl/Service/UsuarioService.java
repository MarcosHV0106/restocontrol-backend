package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.utp.RestoControl.Dto.ActivacionCuentaRequest;
import com.utp.RestoControl.Dto.ActivacionCuentaResponse;
import com.utp.RestoControl.Dto.CambiarClaveRequest;
import com.utp.RestoControl.Dto.LoginRequest;
import com.utp.RestoControl.Dto.LoginResponse;
import com.utp.RestoControl.Dto.UsuarioRequest;
import com.utp.RestoControl.Dto.UsuarioResponse;
import com.utp.RestoControl.Entity.Rol;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Exception.InvalidCredentialsException;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.RolRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository repository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final CuentaActivacionMailService cuentaActivacionMailService;

    @Value("${restocontrol.activation.token-hours:24}")
    private long activationTokenHours;

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repository.findByEliminadoFalse();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Integer id) {
        return repository.findByIdUsuarioAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
    }

    @Transactional
    public Usuario guardar(UsuarioRequest request) {
        validarRequestCreacion(request);

        Usuario usuario = new Usuario();
        usuario.setNombre(normalizarObligatorio(request.getNombre(), "El nombre del usuario es obligatorio."));
        usuario.setApellido(normalizarObligatorio(request.getApellido(), "El apellido del usuario es obligatorio."));
        usuario.setCorreo(normalizarCorreo(request.getCorreo()));
        usuario.setClave(passwordEncoder.encode(generarTokenSeguro(24)));
        usuario.setRol(buscarRolActivo(obtenerIdRol(request)));
        usuario.setDisponible(true);
        usuario.setPendiente(true);
        usuario.setEliminado(false);

        String tokenPlano = prepararTokenActivacion(usuario);

        Usuario usuarioGuardado = repository.save(usuario);
        cuentaActivacionMailService.enviarInvitacion(usuarioGuardado, tokenPlano);

        return usuarioGuardado;
    }

    @Transactional
    public Usuario actualizar(Integer id, UsuarioRequest request) {
        validarRequestActualizacion(request, id);

        Usuario usuario = buscarPorId(id);
        usuario.setNombre(normalizarObligatorio(request.getNombre(), "El nombre del usuario es obligatorio."));
        usuario.setApellido(normalizarObligatorio(request.getApellido(), "El apellido del usuario es obligatorio."));
        usuario.setCorreo(normalizarCorreo(request.getCorreo()));
        usuario.setRol(buscarRolActivo(obtenerIdRol(request)));

        String clave = request.getClave() == null ? null : request.getClave().trim();
        if (!Strings.isNullOrEmpty(clave)) {
            validarLongitudClave(clave);
            usuario.setClave(passwordEncoder.encode(clave));
            usuario.setPendiente(false);
            limpiarTokenActivacion(usuario);
        }

        return repository.save(usuario);
    }

    @Transactional
    public void eliminar(Integer id) {
        Usuario usuario = buscarPorId(id);
        usuario.setEliminado(true);
        repository.save(usuario);
    }

    @Transactional
    public Usuario reenviarActivacion(Integer id) {
        Usuario usuario = buscarPorId(id);
        Preconditions.checkArgument(
                Boolean.TRUE.equals(usuario.getPendiente()),
                "Solo se puede reenviar la invitacion de una cuenta pendiente.");

        String tokenPlano = prepararTokenActivacion(usuario);
        Usuario usuarioGuardado = repository.save(usuario);
        cuentaActivacionMailService.enviarInvitacion(usuarioGuardado, tokenPlano);

        return usuarioGuardado;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        validarLogin(request);

        Usuario usuario = repository.findByCorreoIgnoreCaseAndEliminadoFalse(normalizarCorreo(request.getCorreo()))
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales invalidas."));

        validarUsuarioPuedeAutenticarse(usuario);

        if (!passwordEncoder.matches(request.getClave().trim(), usuario.getClave())) {
            throw new InvalidCredentialsException("Credenciales invalidas.");
        }

        return new LoginResponse("Login correcto.", UsuarioResponse.from(usuario));
    }

    @Transactional(readOnly = true)
    public ActivacionCuentaResponse validarTokenActivacion(String token) {
        Usuario usuario = buscarUsuarioPorTokenValido(token);
        return ActivacionCuentaResponse.pendiente(usuario);
    }

    @Transactional
    public ActivacionCuentaResponse crearClaveActivacion(String token, ActivacionCuentaRequest request) {
        Usuario usuario = buscarUsuarioPorTokenValido(token);
        String clave = validarClavesActivacion(request);

        usuario.setClave(passwordEncoder.encode(clave));
        usuario.setPendiente(false);
        usuario.setDisponible(true);
        limpiarTokenActivacion(usuario);

        return ActivacionCuentaResponse.activada(repository.save(usuario));
    }

    @Transactional
    public void cambiarContrasena(Integer idUsuario, String claveActual, String claveNueva) {
        Usuario usuario = buscarPorId(idUsuario);
        validarUsuarioPuedeAutenticarse(usuario);

        if (!passwordEncoder.matches(claveActual, usuario.getClave())) {
            throw new InvalidCredentialsException("La contrasena actual es incorrecta.");
        }

        String claveValidada = normalizarObligatorio(claveNueva, "La nueva contrasena es obligatoria.");
        validarLongitudClave(claveValidada);

        usuario.setClave(passwordEncoder.encode(claveValidada));
        repository.save(usuario);
    }

    @Transactional
    public void resetearContrasena(Integer idUsuario, String claveNueva) {
        Usuario usuario = buscarPorId(idUsuario);

        String claveValidada = normalizarObligatorio(claveNueva, "La nueva contrasena es obligatoria.");
        validarLongitudClave(claveValidada);

        usuario.setClave(passwordEncoder.encode(claveValidada));
        usuario.setPendiente(false);
        usuario.setDisponible(true);
        limpiarTokenActivacion(usuario);
        repository.save(usuario);
    }

    private void validarRequestCreacion(UsuarioRequest request) {
        validarRequestBase(request, null);
    }

    private void validarRequestActualizacion(UsuarioRequest request, Integer idActual) {
        validarRequestBase(request, idActual);
    }

    private void validarRequestBase(UsuarioRequest request, Integer idActual) {
        Preconditions.checkArgument(request != null, "El usuario es obligatorio.");
        normalizarObligatorio(request.getNombre(), "El nombre del usuario es obligatorio.");
        normalizarObligatorio(request.getApellido(), "El apellido del usuario es obligatorio.");
        String correo = normalizarCorreo(request.getCorreo());
        Preconditions.checkArgument(obtenerIdRol(request) != null, "El rol del usuario es obligatorio.");

        boolean correoDuplicado = idActual == null
                ? repository.existsByCorreoIgnoreCaseAndEliminadoFalse(correo)
                : repository.existsByCorreoIgnoreCaseAndIdUsuarioNotAndEliminadoFalse(correo, idActual);

        Preconditions.checkArgument(!correoDuplicado, "Ya existe un usuario con ese correo.");
    }

    private void validarLogin(LoginRequest request) {
        Preconditions.checkArgument(request != null, "Las credenciales son obligatorias.");
        normalizarCorreo(request.getCorreo());
        normalizarObligatorio(request.getClave(), "La clave es obligatoria.");
    }

    private void validarUsuarioPuedeAutenticarse(Usuario usuario) {
        if (Boolean.TRUE.equals(usuario.getPendiente())) {
            throw new InvalidCredentialsException("La cuenta esta pendiente de activacion. Revisa tu correo.");
        }

        if (Boolean.FALSE.equals(usuario.getDisponible())) {
            throw new InvalidCredentialsException("Usuario inactivo.");
        }

        if (Boolean.TRUE.equals(usuario.getRol().getEliminado())) {
            throw new InvalidCredentialsException("El rol del usuario esta inactivo.");
        }
    }

    private Usuario buscarUsuarioPorTokenValido(String token) {
        String tokenNormalizado = normalizarObligatorio(token, "El token de activacion es obligatorio.");

        Usuario usuario = repository.findByTokenActivacionHashAndEliminadoFalse(hashearToken(tokenNormalizado))
                .orElseThrow(() -> new IllegalArgumentException("El enlace de activacion no es valido."));

        if (!Boolean.TRUE.equals(usuario.getPendiente())) {
            throw new IllegalArgumentException("La cuenta ya fue activada.");
        }

        if (usuario.getTokenActivacionExpira() == null
                || usuario.getTokenActivacionExpira().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El enlace de activacion ha expirado.");
        }

        return usuario;
    }

    private String validarClavesActivacion(ActivacionCuentaRequest request) {
        Preconditions.checkArgument(request != null, "Los datos de activacion son obligatorios.");
        String clave = normalizarObligatorio(request.getClave(), "La contrasena es obligatoria.");
        String confirmacion = normalizarObligatorio(
                request.getConfirmacionClave(),
                "La confirmacion de contrasena es obligatoria.");

        Preconditions.checkArgument(clave.equals(confirmacion), "Las contrasenas no coinciden.");
        validarLongitudClave(clave);

        return clave;
    }

    private void validarLongitudClave(String clave) {
        Preconditions.checkArgument(clave.length() >= 6, "La contrasena debe tener al menos 6 caracteres.");
    }

    private Rol buscarRolActivo(Integer idRol) {
        Preconditions.checkArgument(idRol != null, "El rol es obligatorio.");
        return rolRepository.findByIdRolAndEliminadoFalse(idRol)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado."));
    }

    private Integer obtenerIdRol(UsuarioRequest request) {
        if (request.getIdRol() != null) {
            return request.getIdRol();
        }

        return request.getRol() == null ? null : request.getRol().getIdRol();
    }

    private String normalizarCorreo(String correo) {
        String normalizado = normalizarObligatorio(correo, "El correo es obligatorio.")
                .toLowerCase(Locale.ROOT);
        Preconditions.checkArgument(normalizado.contains("@"), "El correo no tiene un formato valido.");
        return normalizado;
    }

    private String normalizarObligatorio(String valor, String mensaje) {
        String normalizado = valor == null ? null : valor.trim();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(normalizado), mensaje);
        return normalizado;
    }

    private void limpiarTokenActivacion(Usuario usuario) {
        usuario.setTokenActivacionHash(null);
        usuario.setTokenActivacionExpira(null);
    }

    private String prepararTokenActivacion(Usuario usuario) {
        String tokenPlano = generarTokenSeguro(32);
        usuario.setTokenActivacionHash(hashearToken(tokenPlano));
        usuario.setTokenActivacionExpira(LocalDateTime.now().plusHours(obtenerHorasExpiracion()));
        return tokenPlano;
    }

    private long obtenerHorasExpiracion() {
        return activationTokenHours > 0 ? activationTokenHours : 24;
    }

    private String generarTokenSeguro(int cantidadBytes) {
        byte[] bytes = new byte[cantidadBytes];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashearToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo generar el hash del token de activacion.", ex);
        }
    }

    @Transactional
    public void cambiarContrasenaPerfil(Integer idUsuario, CambiarClaveRequest request) {

        Preconditions.checkArgument(request != null, "La solicitud es obligatoria.");

        Preconditions.checkArgument(
                request.getClaveNueva().equals(request.getConfirmarClave()),
                "Las contraseñas no coinciden.");

        Preconditions.checkArgument(
                !request.getClaveActual().equals(request.getClaveNueva()),
                "La nueva contraseña debe ser diferente a la actual.");

        Usuario usuario = buscarPorId(idUsuario);

        validarUsuarioPuedeAutenticarse(usuario);

        if (!passwordEncoder.matches(request.getClaveActual(), usuario.getClave())) {
            throw new IllegalArgumentException(
                    "La contraseña actual es incorrecta.");
        }

        validarLongitudClave(request.getClaveNueva());

        usuario.setClave(passwordEncoder.encode(request.getClaveNueva()));

        repository.save(usuario);
    }
}
