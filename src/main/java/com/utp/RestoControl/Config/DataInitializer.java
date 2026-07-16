package com.utp.RestoControl.Config;

import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class DataInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            try {
                var usuarios = usuarioRepository.findByEliminadoFalse();
                
                boolean hayActualizaciones = false;
                
                for (Usuario usuario : usuarios) {
                    if (usuario.getPendiente() == null) {
                        usuario.setPendiente(false);
                        usuarioRepository.save(usuario);
                        hayActualizaciones = true;
                    }

                    if (usuario.getClave() != null && !esHashBCrypt(usuario.getClave())) {
                        usuario.setClave(passwordEncoder.encode(usuario.getClave()));
                        usuarioRepository.save(usuario);
                        hayActualizaciones = true;
                    }
                }
                
                if (hayActualizaciones) {
                    LOGGER.info("Inicializacion de usuarios completada resultado=ACTUALIZADO");
                } else {
                    LOGGER.info("Inicializacion de usuarios completada resultado=SIN_CAMBIOS");
                }
                
            } catch (Exception e) {
                LOGGER.error("No se pudo completar la inicializacion de usuarios", e);
            }
        };
    }

    private boolean esHashBCrypt(String hash) {
        if (hash == null || hash.length() < 4) {
            return false;
        }
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }
}
