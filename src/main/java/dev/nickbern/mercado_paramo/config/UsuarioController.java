package dev.nickbern.mercado_paramo.config;

import dev.nickbern.mercado_paramo.dto.PerfilResponse;
import dev.nickbern.mercado_paramo.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/me")
    public ResponseEntity<PerfilResponse> me(@AuthenticationPrincipal UserDetails principal){
        return ResponseEntity.ok(usuarioService.perfil(principal.getUsername()));
    }

}
