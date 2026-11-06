package br.com.abadeus.paellaapi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<ErroResposta> naoEncontrado(NaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, "Nao encontrado", e.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResposta> regraNegocio(RegraNegocioException e) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negocio", e.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResposta> conflito(ConflitoException e) {
        return resposta(HttpStatus.CONFLICT, "Conflito", e.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResposta> acessoNegado(AcessoNegadoException e) {
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado", e.getMessage());
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErroResposta> acessoNegadoSpring(RuntimeException e) {
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado", "Seu perfil nao tem permissao para esta acao.");
    }

    @ExceptionHandler(NaoAutenticadoException.class)
    public ResponseEntity<ErroResposta> naoAutenticado(NaoAutenticadoException e) {
        return resposta(HttpStatus.UNAUTHORIZED, "Nao autenticado", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ErroResposta corpo = new ErroResposta(400, "Dados invalidos",
                "Verifique os campos informados.", LocalDateTime.now(), campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroResposta> corpoInvalido(Exception e) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisicao invalida",
                "JSON ou parametro em formato invalido. Confira datas (aaaa-MM-ddTHH:mm:ss), numeros e valores de lista.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResposta> parametroAusente(MissingServletRequestParameterException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisicao invalida",
                "Parametro obrigatorio ausente: " + e.getParameterName());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResposta> rotaInexistente(NoResourceFoundException e) {
        return resposta(HttpStatus.NOT_FOUND, "Nao encontrado", "Rota inexistente: /" + e.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResposta> metodoNaoSuportado(HttpRequestMethodNotSupportedException e) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Metodo nao permitido",
                "Esta rota nao aceita o metodo " + e.getMethod() + ".");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> integridade(DataIntegrityViolationException e) {
        log.warn("Violacao de integridade: {}", e.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "Conflito",
                "Operacao viola uma restricao do banco (registro duplicado ou referencia invalida).");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> generico(Exception e) {
        log.error("Erro inesperado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado no servidor.");
    }

    private ResponseEntity<ErroResposta> resposta(HttpStatus status, String erro, String mensagem) {
        return ResponseEntity.status(status).body(ErroResposta.de(status.value(), erro, mensagem));
    }
}
