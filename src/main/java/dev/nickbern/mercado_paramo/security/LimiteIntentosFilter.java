package dev.nickbern.mercado_paramo.security;

import dev.nickbern.mercado_paramo.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LimiteIntentosFilter extends OncePerRequestFilter {

    private static final Set<String> RUTAS_LIMITADAS = Set.of("/api/auth/login", "/api/auth/registro");
    private static final int MAX_CLAVES = 10_000;

    private final Map<String, Deque<Instant>> intentos = new ConcurrentHashMap<>();
    private final JsonMapper jsonMapper;
    private final int maxIntentos;
    private final Duration ventana;

    public LimiteIntentosFilter(JsonMapper jsonMapper,
                                @Value("${app.limite-intentos.max}") int maxIntentos,
                                @Value("${app.limite-intentos.ventana-segundos}") long ventanaSegundos) {
        this.jsonMapper = jsonMapper;
        this.maxIntentos = maxIntentos;
        this.ventana = Duration.ofSeconds(ventanaSegundos);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && RUTAS_LIMITADAS.contains(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Instant ahora = Instant.now();
        purgarSiCrecioDemasiado(ahora);

        String clave = request.getRequestURI() + "|" + request.getRemoteAddr();
        Deque<Instant> marcas = intentos.computeIfAbsent(clave, k -> new ArrayDeque<>());

        long segundosParaReintentar = 0;
        synchronized (marcas) {
            descartarVencidas(marcas, ahora);
            if (marcas.size() >= maxIntentos) {
                segundosParaReintentar = Duration.between(ahora, marcas.peekFirst().plus(ventana)).toSeconds() + 1;
            } else {
                marcas.addLast(ahora);
            }
        }

        if (segundosParaReintentar > 0) {
            responderDemasiadosIntentos(request, response, segundosParaReintentar);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void descartarVencidas(Deque<Instant> marcas, Instant ahora) {
        Instant limite = ahora.minus(ventana);
        while (!marcas.isEmpty() && marcas.peekFirst().isBefore(limite)) {
            marcas.pollFirst();
        }
    }

    private void purgarSiCrecioDemasiado(Instant ahora) {
        if (intentos.size() > MAX_CLAVES) {
            intentos.entrySet().removeIf(entrada -> {
                synchronized (entrada.getValue()) {
                    descartarVencidas(entrada.getValue(), ahora);
                    return entrada.getValue().isEmpty();
                }
            });
        }
    }

    private void responderDemasiadosIntentos(HttpServletRequest request,
                                             HttpServletResponse response,
                                             long segundos) throws IOException {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("Demasiados intentos. Intenta de nuevo en %d segundos".formatted(segundos))
                .path(request.getRequestURI())
                .errores(Map.of())
                .build();

        response.setStatus(status.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(segundos));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}