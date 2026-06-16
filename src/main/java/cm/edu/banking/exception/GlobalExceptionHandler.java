package cm.edu.banking.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestionnaire global d'exceptions REST pour l'ensemble de l'API bancaire.
 *
 * <p>Intercepte les exceptions levées dans les contrôleurs et les services
 * et les transforme en réponses JSON uniformes, incluant le code HTTP
 * approprié, un horodatage, et un message d'erreur lisible par le client.</p>
 *
 * <p>Format de réponse d'erreur standard :</p>
 * <pre>{@code
 * {
 *   "timestamp": "2024-01-15T10:30:00",
 *   "status": 400,
 *   "error": "Requête invalide",
 *   "message": "Solde insuffisant pour effectuer cette opération",
 *   "path": "/api/operations/retrait"
 * }
 * }</pre>
 *
 * @author MEZT-coding
 * @since 1.0.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // =========================================================================
    //  Exceptions métier
    // =========================================================================

    /**
     * Gère les violations de règles métier (Skill 9, 10, 11, 12, etc.).
     */
    @ExceptionHandler(BankingException.class)
    public ResponseEntity<Map<String, Object>> handleBankingException(
            BankingException ex, WebRequest request) {
        log.warn("Règle métier violée : {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Règle métier violée",
                ex.getMessage(), request);
    }

    /**
     * Gère les ressources introuvables (utilisateur, banque, compte, etc.).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Ressource introuvable : {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Ressource introuvable",
                ex.getMessage(), request);
    }

    /**
     * Gère les tentatives de création de ressources dupliquées (email,
     * téléphone, nom de banque).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResource(
            DuplicateResourceException ex, WebRequest request) {
        log.warn("Duplication détectée : {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflit de données",
                ex.getMessage(), request);
    }

    // =========================================================================
    //  Exceptions de sécurité
    // =========================================================================

    /**
     * Gère les tentatives d'accès refusées (rôle insuffisant —
     * {@code 403 Forbidden}).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        log.warn("Accès refusé : {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Accès refusé",
                "Vous ne disposez pas des droits nécessaires pour effectuer cette action.",
                request);
    }

    /**
     * Gère les identifiants incorrects lors de la connexion.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {
        log.warn("Tentative de connexion avec des identifiants incorrects");
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentification échouée",
                "Email ou mot de passe incorrect.", request);
    }

    // =========================================================================
    //  Erreurs de validation Bean Validation (@Valid)
    // =========================================================================

    /**
     * Gère les erreurs de validation des DTOs annotés {@code @Valid}.
     *
     * <p>Retourne un objet JSON enrichi listant tous les champs invalides
     * et leurs messages d'erreur respectifs sous la clé {@code validationErrors}.</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "Valeur invalide",
                        (existing, replacement) -> existing
                ));

        log.warn("Erreurs de validation : {}", validationErrors);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Données invalides");
        body.put("message", "Un ou plusieurs champs sont invalides. "
                + "Veuillez corriger les erreurs listées.");
        body.put("validationErrors", validationErrors);
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // =========================================================================
    //  Erreur générique (dernier recours)
    // =========================================================================

    /**
     * Capture toute exception non gérée par les handlers précédents et
     * retourne une erreur générique {@code 500 Internal Server Error}.
     *
     * <p>Le message interne de l'exception n'est jamais exposé au client
     * pour des raisons de sécurité (pas de fuite de stack trace).</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Erreur interne inattendue : {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erreur interne du serveur",
                "Une erreur inattendue est survenue. Veuillez réessayer ultérieurement.",
                request);
    }

    // =========================================================================
    //  Méthode utilitaire
    // =========================================================================

    /**
     * Construit le corps JSON standardisé des réponses d'erreur.
     *
     * @param status  code HTTP à retourner
     * @param error   libellé court décrivant le type d'erreur
     * @param message message détaillé destiné au client de l'API
     * @param request contexte de la requête HTTP (pour extraire le chemin)
     * @return la réponse HTTP avec le corps JSON d'erreur
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String error, String message, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(status).body(body);
    }
}
