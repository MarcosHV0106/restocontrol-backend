package com.utp.RestoControl.Config;

import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Inicializador de datos para RestoControl
 * 
 * Se ejecuta al iniciar la aplicación para:
 * - Hashear contraseñas de usuarios si aún no están hasheadas
 * - Inicializar datos de prueba (si es necesario)
 */
@Configuration
public class DataInitializer {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Bean que se ejecuta al iniciar la aplicación
     * Verifica y hashea las contraseñas de los usuarios
     */
    @Bean
    public CommandLineRunner initData() {
        return args -> {
            try {
                // Obtener todos los usuarios
                var usuarios = usuarioRepository.findByEliminadoFalse();
                
                boolean hayActualizaciones = false;
                
                // Verificar si las contraseñas necesitan ser hasheadas
                for (Usuario usuario : usuarios) {
                    if (usuario.getClave() != null && !esHashBCrypt(usuario.getClave())) {
                        System.out.println("Hasheando contraseña para usuario: " + usuario.getCorreo());
                        usuario.setClave(passwordEncoder.encode(usuario.getClave()));
                        usuarioRepository.save(usuario);
                        hayActualizaciones = true;
                    }
                }
                
                if (hayActualizaciones) {
                    System.out.println("✓ Contraseñas hasheadas correctamente");
                } else {
                    System.out.println("✓ Todas las contraseñas ya están hasheadas");
                }
                
            } catch (Exception e) {
                System.err.println("Error al inicializar datos: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    /**
     * Verificar si una contraseña está en formato BCrypt
     * Las contraseñas BCrypt comienzan con $2a$, $2b$ o $2y$
     * 
     * @param hash Hash a verificar
     * @return true si es BCrypt, false en caso contrario
     */
    private boolean esHashBCrypt(String hash) {
        if (hash == null || hash.length() < 4) {
            return false;
        }
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }
}
