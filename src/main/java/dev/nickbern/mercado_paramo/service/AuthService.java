package dev.nickbern.mercado_paramo.service;
import dev.nickbern.mercado_paramo.dto.LoginRequest;
import dev.nickbern.mercado_paramo.dto.LoginResponse;
import dev.nickbern.mercado_paramo.dto.RegistroRequest;
import dev.nickbern.mercado_paramo.dto.RegistroResponse;
import dev.nickbern.mercado_paramo.exception.RecursoDuplicadoException;
import dev.nickbern.mercado_paramo.exception.ReglaNegocioException;
import dev.nickbern.mercado_paramo.model.EstadoVerificacion;
import dev.nickbern.mercado_paramo.model.Productor;
import dev.nickbern.mercado_paramo.model.Rol;
import dev.nickbern.mercado_paramo.model.Usuario;
import dev.nickbern.mercado_paramo.repository.ProductorRepository;
import dev.nickbern.mercado_paramo.repository.UsuarioRepository;
import dev.nickbern.mercado_paramo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ProductorRepository productorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public RegistroResponse registro(RegistroRequest request){

        if (Rol.ADMIN == request.rol()){
            throw new ReglaNegocioException("No se permite el registro con rol ADMIN");
        }

        String email = request.email().strip().toLowerCase();

        if (usuarioRepository.existsByEmail(email)) {
            throw new RecursoDuplicadoException("El email ya está registrado");
        }

        if (Rol.PRODUCTOR == request.rol() && (request.vereda() == null || request.vereda().isBlank())) {
            throw new ReglaNegocioException("Para 'PRODUCTOR' la vereda es obligatoria");
        }

        Usuario usuario = new Usuario(
                request.nombre(),
                email,
                passwordEncoder.encode(request.password()),
                request.telefono(),
                request.rol()
        );

        Usuario usuarioGuardado =  usuarioRepository.save(usuario);

        Productor productor = null;

        if (Rol.PRODUCTOR == usuarioGuardado.getRol()){

            Productor newProductor = new Productor(
                    usuarioGuardado,
                    request.vereda(),
                    request.descripcion(),
                    EstadoVerificacion.PENDIENTE
            );

            productor = productorRepository.save(newProductor);
        }

        return RegistroResponse.de(usuarioGuardado, productor);
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(), request.password()));

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generarToken(principal);
        return new LoginResponse(token, "Bearer", jwtService.getExpiracionSegundos());
    }
}
