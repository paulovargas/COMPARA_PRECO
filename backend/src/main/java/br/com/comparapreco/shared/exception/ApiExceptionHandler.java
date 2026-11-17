package br.com.comparapreco.shared.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import br.com.comparapreco.autenticacao.service.LimiteAcessoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ErroNegocioException.class)
    public ResponseEntity<ErroResponse> negocio(ErroNegocioException exception) {
        return ResponseEntity.status(exception.getStatus()).body(ErroResponse.de(
                exception.getStatus(), exception.getCodigo(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException exception) {
        var campos = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErroResponse(Instant.now(), 400, "DADOS_INVALIDOS",
                "Confira os campos informados.", campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroResponse> formato(Exception exception) {
        return ResponseEntity.badRequest().body(ErroResponse.de(400, "REQUISICAO_INVALIDA", "Formato de requisição inválido."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> autenticacao(AuthenticationException exception) {
        return ResponseEntity.status(401).body(ErroResponse.de(401, "CREDENCIAIS_INVALIDAS", "Email ou senha inválidos."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> acesso(AccessDeniedException exception) {
        return ResponseEntity.status(403).body(ErroResponse.de(403, "ACESSO_NEGADO", "Você não tem permissão para esta operação."));
    }

    @ExceptionHandler(LimiteAcessoException.class)
    public ResponseEntity<ErroResponse> limite(LimiteAcessoException exception) {
        return ResponseEntity.status(429).header("Retry-After", Integer.toString(exception.getTentarNovamenteEm()))
                .body(ErroResponse.de(429, "LIMITE_ACESSO", exception.getMessage()));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ErroResponse> conflito(Exception exception) {
        return ResponseEntity.status(409).body(ErroResponse.de(409, "CONFLITO", "A operação conflita com os dados atuais."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception exception) {
        if (exception instanceof org.springframework.web.ErrorResponse erro) {
            int status = erro.getStatusCode().value();
            return ResponseEntity.status(status).body(ErroResponse.de(status, "REQUISICAO_INVALIDA", "Requisição não pôde ser processada."));
        }
        LOGGER.error("Falha inesperada ao processar a requisição.", exception);
        return ResponseEntity.internalServerError().body(ErroResponse.de(500, "ERRO_INTERNO", "Não foi possível concluir a operação."));
    }
}
