package dev.nickbern.mercado_paramo.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(GlobalExceptionHandlerTest.ErroresController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // --- Controlador falso: solo existe dentro de esta prueba ---
    @RestController
    @RequestMapping("/test")
    static class ErroresController {

        @GetMapping("/no-encontrado")
        void noEncontrado() {
            throw new RecursoNoEncontradoException("Pedido no encontrado");
        }

        // tú agregas: /conflicto, /validacion (POST), /error-interno
    }

    // --- Pruebas ---
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
}
