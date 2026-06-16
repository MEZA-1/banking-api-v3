package cm.edu.banking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Point d'entrée d'authentification Spring Security, invoqué lorsqu'une
 * requête non authentifiée tente d'accéder à une ressource protégée.
 *
 * <p>Au lieu du comportement par défaut de Spring Security (redirection
 * vers un formulaire de connexion HTML), ce composant retourne une réponse
 * JSON avec le code HTTP {@code 401 Unauthorized}, ce qui est le
 * comportement attendu d'une API REST stateless.</p>
 *
 * <p>Format de la réponse d'erreur retournée :</p>
 * <pre>{@code
 * {
 *   "timestamp": "2024-01-15T10:30:00",
 *   "status": 401,
 *   "error": "Non autorisé",
 *   "message": "Accès refusé : authentification requise",
 *   "path": "/api/banques"
 * }
 * }</pre>
 *
 * @author Équipe Banking API
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Gère les tentatives d'accès non authentifiées à une ressource
     * protégée en renvoyant une réponse JSON {@code 401 Unauthorized}.
     *
     * @param request       la requête HTTP ayant déclenché l'erreur
     * @param response      la réponse HTTP à construire
     * @param authException l'exception d'authentification à l'origine du
     *                      rejet
     * @throws IOException en cas d'erreur d'écriture dans la réponse HTTP
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        log.warn("Tentative d'accès non authentifié à {} : {}",
                request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Non autorisé");
        body.put("message", "Accès refusé : authentification requise. "
                + "Veuillez fournir un token JWT valide dans l'en-tête Authorization.");
        body.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
