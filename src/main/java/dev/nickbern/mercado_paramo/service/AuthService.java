package dev.nickbern.mercado_paramo.service;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ProductorRepository productorRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegistroResponse registro(RegistroRequest request){

        if (Rol.ADMIN.name().equalsIgnoreCase(request.rol()))
            throw new ReglaNegocioException("Rol inválido");

        if (usuarioRepository.existsByEmail(request.email().strip().toLowerCase()))
            throw new RecursoDuplicadoException(request.email().strip().toLowerCase() + " - Ya se encuentra registrado");

        if (Rol.PRODUCTOR.name().equalsIgnoreCase(request.rol()) && request.vereda() == null)
            throw new ReglaNegocioException("Para 'PRODUCTOR' la vereda es obligatoria");

        Usuario newUser = new Usuario(
                request.nombre(),
                request.email().strip().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.telefono(),
                setRol(request.rol())
        );

        Usuario userSaved =  usuarioRepository.save(newUser);

        Productor productor = null;

        if (Rol.PRODUCTOR.name().equalsIgnoreCase(userSaved.getRol().name())){

            Productor newProductor = new Productor(
                    userSaved,
                    request.vereda(),
                    request.descripcion(),
                    EstadoVerificacion.PENDIENTE
            );

            productor = productorRepository.save(newProductor);
        }

        return RegistroResponse.de(userSaved, productor);
    }

    public static Rol setRol(String inputRol){
        if (Rol.PRODUCTOR.name().equalsIgnoreCase(inputRol)){
            return Rol.PRODUCTOR;
        } else {
            return Rol.COMPRADOR;
        }
    }
}
