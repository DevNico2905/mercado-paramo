package dev.nickbern.mercado_paramo.exception;

import dev.nickbern.mercado_paramo.model.Producto;
import dev.nickbern.mercado_paramo.security.JwtService;
import dev.nickbern.mercado_paramo.security.UsuarioDetailsService;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ErroresController.class)
@Import(GlobalExceptionHandlerTest.ErroresController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @RestController
    @RequestMapping("/test")
    static class ErroresController {

        @GetMapping("/no-encontrado")
        void noEncontrado() {
            throw new RecursoNoEncontradoException("Pedido no encontrado");
        }

        @GetMapping("/conflicto")
        void conflicto() {
            throw new StockInsuficienteException("Stock insuficiente para: Queso campesino");
        }

        @PostMapping("/validacion")
        void validacion(@Valid @RequestBody DatoPrueba request){

        }

        @GetMapping("/error-interno")
        void errorInterno() {
            throw new RuntimeException("detalle secreto de la BD");
        }

        @GetMapping("/concurrencia")
        void concurrencia() {
            throw new ObjectOptimisticLockingFailureException(Producto.class, 1L);
        }

        @GetMapping("/numero/{id}")
        void numero(@PathVariable Long id) {
        }
    }

    @Test
    void recursoNoEncontrado_devuelve404ConFormatoEstandar() throws Exception {
        mockMvc.perform(get("/test/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Pedido no encontrado"))
                .andExpect(jsonPath("$.path").value("/test/no-encontrado"))
                .andExpect(jsonPath("$.errores").isEmpty());
    }

    @Test
    void stockInsuficiente_devuelve409ConFormatoEstandar() throws Exception {
        mockMvc.perform(get("/test/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Stock insuficiente para: Queso campesino"))
                .andExpect(jsonPath("$.path").value("/test/conflicto"))
                .andExpect(jsonPath("$.errores").isEmpty());
    }

    @Test
    void campoInvalido_devuelve400ConErroresPorCampo() throws Exception{
        mockMvc.perform(post("/test/validacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("La petición tiene campos inválidos"))
                .andExpect(jsonPath("$.path").value("/test/validacion"))
                .andExpect(jsonPath("$.errores.nombre").exists());

    }

    @Test
    void errorInesperado_devuelve500SinExponerDetalles() throws Exception{
        mockMvc.perform(get("/test/error-interno"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Ocurrió un error inesperado en el servidor"))
                .andExpect(jsonPath("$.path").value("/test/error-interno"))
                .andExpect(jsonPath("$.errores").isEmpty())
                .andExpect(content().string(not(containsString("secreto"))));

    }

    @Test
    void rutaInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/test/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("El recurso solicitado no existe"))
                .andExpect(jsonPath("$.path").value("/test/no-existe"))
                .andExpect(jsonPath("$.errores").isEmpty());
    }

    @Test
    void conflictoDeConcurrencia_devuelve409() throws Exception {
        mockMvc.perform(get("/test/concurrencia"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void parametroConTipoInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/test/numero/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parámetro 'id' tiene un formato inválido"));
    }
}
