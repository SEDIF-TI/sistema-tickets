package com.sedif.sistema_tickets.util.config; // Ajuste el paquete según su estructura

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.util.enums.RolUsuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        
        // 1. Verificamos si el administrador ya existe en la base de datos
        String correoAdmin = "admin@sedif.com";
        String usernameAdmin = "admin_pruebas";

        if (usuarioRepository.findByCorreoOrUsername(correoAdmin, usernameAdmin).isEmpty()) {
            
            log.info("Iniciando el sembrado de datos (Database Seeding)...");

            // 2. Construimos la entidad del usuario
            Usuario admin = new Usuario();
            admin.setNombre("Administrador de Pruebas");
            admin.setCorreo(correoAdmin);
            admin.setUsername(usernameAdmin);
            
            // 3. LA MAGIA: Java encripta la contraseña antes de mandarla a PostgreSQL
            admin.setPassword(passwordEncoder.encode("admin123"));
            
            // Reemplace ROL_ADMINISTRADOR por el valor exacto de su Enum RolUsuario
            admin.setRol(RolUsuario.ADMINISTRADOR); 
            
            admin.setActivo(true);
            admin.setDisponibleSoporte(false);

            // 4. Guardamos en la base de datos
            usuarioRepository.save(admin);
            
            log.info("✅ Usuario administrador creado y encriptado automáticamente.");
        } else {
            log.info("✅ El usuario administrador ya existe. Omitiendo sembrado.");
        }
    }
}