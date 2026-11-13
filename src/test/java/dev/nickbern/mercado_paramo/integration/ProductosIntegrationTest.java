package dev.nickbern.mercado_paramo.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductosIntegrationTest {

    private static final String URL_PRODUCTOR = "/api/productor/productos";
    private static final String PASSWORD = "clave-segura-123";

    @Autowired
    private MockMvc mockMvc;

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String login(String email, String password) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private void registrar(String nombre, String email, String rol) throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","email":"%s","password":"%s","telefono":"3001234567",
                                 "rol":"%s","vereda":"La Concepción"}
                                """.formatted(nombre, email, PASSWORD, rol)))
                .andExpect(status().isCreated());
    }

    private String productorVerificado(String nombre, String email) throws Exception {
        registrar(nombre, email, "PRODUCTOR");

        String tokenAdmin = login("admin@mercado-paramo.local", "admin-test-123");
        String pendientes = mockMvc.perform(get("/api/admin/productores").param("estado", "PENDIENTE")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andReturn().getResponse().getContentAsString();
        Integer idProductor = JsonPath.read(pendientes, "$[0].id");

        mockMvc.perform(patch("/api/admin/productores/{id}/verificacion", idProductor)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"VERIFICADO"}
                                """))
                .andExpect(status().isOk());

        return login(email, PASSWORD);
    }

    private Integer idCategoria(String nombre) throws Exception {
        String respuesta = mockMvc.perform(get("/api/categorias"))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(respuesta, "$[?(@.nombre == '%s')].id".formatted(nombre));
        return ids.get(0);
    }

    private String productoJson(Integer categoriaId, String precio) {
        return """
                {"nombre":"Queso campesino","unidad":"libra","precio":%s,"stock":20,"categoriaId":%d}
                """.formatted(precio, categoriaId);
    }

    private Integer crearProducto(String token, Integer categoriaId) throws Exception {
        String respuesta = mockMvc.perform(post(URL_PRODUCTOR)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson(categoriaId, "12000")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    @Test
    void categorias_sonPublicas() throws Exception {
        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void catalogo_sinToken_muestraProductoYFiltraPorCategoria() throws Exception {
        String token = productorVerificado("Pedro", "pedro@correo.com");
        Integer lacteos = idCategoria("Lácteos");
        crearProducto(token, lacteos);

        mockMvc.perform(get("/api/productos").param("categoriaId", lacteos.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Queso campesino"))
                .andExpect(jsonPath("$[0].productorNombre").value("Pedro"));

        mockMvc.perform(get("/api/productos").param("categoriaId", idCategoria("Frutas").toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void crear_productorVerificado_devuelve201() throws Exception {
        String token = productorVerificado("Pedro", "pedro@correo.com");

        mockMvc.perform(post(URL_PRODUCTOR)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson(idCategoria("Lácteos"), "12000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stock").value(20))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.categoriaNombre").value("Lácteos"))
                .andExpect(jsonPath("$.vereda").value("La Concepción"));
    }

    @Test
    void crear_productorPendiente_devuelve403() throws Exception {
        registrar("Pedro", "pedro@correo.com", "PRODUCTOR");
        String token = login("pedro@correo.com", PASSWORD);

        mockMvc.perform(post(URL_PRODUCTOR)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson(idCategoria("Lácteos"), "12000")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tu cuenta de productor aún no ha sido verificada"));
    }

    @Test
    void crear_comoComprador_devuelve403() throws Exception {
        registrar("María", "maria@correo.com", "COMPRADOR");
        String token = login("maria@correo.com", PASSWORD);

        mockMvc.perform(post(URL_PRODUCTOR)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson(idCategoria("Lácteos"), "12000")))
                .andExpect(status().isForbidden());
    }

    @Test
    void crear_categoriaInexistente_devuelve404() throws Exception {
        String token = productorVerificado("Pedro", "pedro@correo.com");

        mockMvc.perform(post(URL_PRODUCTOR)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson(999_999, "12000")))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_precioCero_devuelve400() throws Exception {
        String token = productorVerificado("Pedro", "pedro@correo.com");

        mockMvc.perform(post(URL_PRODUCTOR)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson(idCategoria("Lácteos"), "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precio").exists());
    }
    
    @Test
    void actualizar_productoDeOtroProductor_devuelve403() throws Exception {
        String tokenPedro = productorVerificado("Pedro", "pedro@correo.com");
        Integer idProducto = crearProducto(tokenPedro, idCategoria("Lácteos"));

        String tokenAna = productorVerificado("Ana", "ana@correo.com");

        mockMvc.perform(put(URL_PRODUCTOR + "/{id}", idProducto)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAna))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson(idCategoria("Lácteos"), "1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No puedes modificar productos de otro productor"));
    }

    @Test
    void desactivar_productoPropio_loQuitaDelCatalogoPeroNoDeMisProductos() throws Exception {
        String token = productorVerificado("Pedro", "pedro@correo.com");
        Integer idProducto = crearProducto(token, idCategoria("Lácteos"));

        mockMvc.perform(delete(URL_PRODUCTOR + "/{id}", idProducto)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/productos"))
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get(URL_PRODUCTOR)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].activo").value(false));
    }

    @Test
    void reactivar_productoOculto_vuelveAlCatalogo() throws Exception {
        String token = productorVerificado("Pedro", "pedro@correo.com");
        Integer idProducto = crearProducto(token, idCategoria("Lácteos"));

        mockMvc.perform(delete(URL_PRODUCTOR + "/{id}", idProducto)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch(URL_PRODUCTOR + "/{id}/reactivar", idProducto)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(get("/api/productos"))
                .andExpect(jsonPath("$.length()").value(1));
    }
}