
package com.utp.RestoControl.Service;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UsuarioService {
    
    private final UsuarioRepository repository;
    private final RolRepository rolRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Transactional(readOnly = true)
    public List<Usuario> listar(){
        return repository.findByEliminadoFalse();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Integer id) {
        return repository.findByIdUsuarioAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
    }

    @Transactional
    public Usuario guardar(UsuarioRequest request){
        validarRequestCreacion(request);

        Usuario usuario = new Usuario();
        usuario.setNombre(normalizarObligatorio(request.getNombre(), "El nombre del usuario es obligatorio."));
        usuario.setApellido(normalizarObligatorio(request.getApellido(), "El apellido del usuario es obligatorio."));
        usuario.setCorreo(normalizarCorreo(request.getCorreo()));
        // Hashear la contraseña al guardar
        usuario.setClave(passwordEncoder.encode(
                normalizarObligatorio(request.getClave(), "La clave del usuario es obligatoria.")));
        usuario.setRol(buscarRolActivo(obtenerIdRol(request)));
        usuario.setDisponible(true);
        usuario.setEliminado(false);

        return repository.save(usuario);
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
            // Hashear la contraseña al actualizar
            usuario.setClave(passwordEncoder.encode(clave));
        }

        return repository.save(usuario);
    }

    @Transactional
    public void eliminar(Integer id) {
        Usuario usuario = buscarPorId(id);
        usuario.setEliminado(true);
        repository.save(usuario);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        validarLogin(request);

        Usuario usuario = repository.findByCorreoIgnoreCaseAndEliminadoFalse(normalizarCorreo(request.getCorreo()))
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales invalidas."));

        if (Boolean.TRUE.equals(usuario.getRol().getEliminado())) {
            throw new InvalidCredentialsException("El rol del usuario esta inactivo.");
        }

        // Usar passwordEncoder para verificar la contraseña
        if (!passwordEncoder.matches(request.getClave().trim(), usuario.getClave())) {
            throw new InvalidCredentialsException("Credenciales invalidas.");
        }

        return new LoginResponse("Login correcto.", UsuarioResponse.from(usuario));
    }

    private void validarRequestCreacion(UsuarioRequest request) {
        validarRequestBase(request, null);
        normalizarObligatorio(request.getClave(), "La clave del usuario es obligatoria.");
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

        Preconditions.checkArgument(!correoDuplicado, "Ya existe un usuario activo con ese correo.");
    }

    private void validarLogin(LoginRequest request) {
        Preconditions.checkArgument(request != null, "Las credenciales son obligatorias.");
        normalizarCorreo(request.getCorreo());
        normalizarObligatorio(request.getClave(), "La clave es obligatoria.");
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

    @Transactional
    public void cambiarContrasena(Integer idUsuario, String claveActual, String claveNueva) {
        Usuario usuario = buscarPorId(idUsuario);
        
        // Validar contraseña actual
        if (!passwordEncoder.matches(claveActual, usuario.getClave())) {
            throw new InvalidCredentialsException("La contraseña actual es incorrecta.");
        }
        
        // Validar nueva contraseña
        String claveValidada = normalizarObligatorio(claveNueva, "La nueva contraseña es obligatoria.");
        Preconditions.checkArgument(claveValidada.length() >= 6, "La nueva contraseña debe tener al menos 6 caracteres.");
        
        // Actualizar contraseña
        usuario.setClave(passwordEncoder.encode(claveValidada));
        repository.save(usuario);
    }

    @Transactional
    public void resetearContrasena(Integer idUsuario, String claveNueva) {
        Usuario usuario = buscarPorId(idUsuario);
        
        // Validar nueva contraseña
        String claveValidada = normalizarObligatorio(claveNueva, "La nueva contraseña es obligatoria.");
        Preconditions.checkArgument(claveValidada.length() >= 6, "La nueva contraseña debe tener al menos 6 caracteres.");
        
        // Actualizar contraseña
        usuario.setClave(passwordEncoder.encode(claveValidada));
        repository.save(usuario);
    }
}
