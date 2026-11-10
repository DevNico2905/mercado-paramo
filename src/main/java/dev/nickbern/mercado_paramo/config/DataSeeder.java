package dev.nickbern.mercado_paramo.config;

import dev.nickbern.mercado_paramo.model.Categoria;
import dev.nickbern.mercado_paramo.model.Rol;
import dev.nickbern.mercado_paramo.model.Usuario;
import dev.nickbern.mercado_paramo.repository.CategoriaRepository;
import dev.nickbern.mercado_paramo.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DataSeeder implements CommandLineRunner {

    private static final List<String> CATEGORIAS = List.of(
            "Tubérculos", "Lácteos", "Frutas", "Hortalizas", "Hierbas y aromáticas");

    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(UsuarioRepository usuarioRepository,
                      CategoriaRepository categoriaRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.admin.email}") String adminEmail,
                      @Value("${app.admin.password}") String adminPassword) {
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail.strip().toLowerCase();
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        sembrarAdmin();
        sembrarCategorias();
    }

    private void sembrarAdmin() {
        if (usuarioRepository.existsByEmail(adminEmail)) {
            return;
        }
        usuarioRepository.save(new Usuario(
                "Administrador",
                adminEmail,
                passwordEncoder.encode(adminPassword),
                "0000000000",
                Rol.ADMIN));
        log.info("Usuario ADMIN creado: {}", adminEmail);
    }

    private void sembrarCategorias() {
        CATEGORIAS.stream()
                .filter(nombre -> !categoriaRepository.existsByNombre(nombre))
                .map(Categoria::new)
                .forEach(categoriaRepository::save);
    }
}