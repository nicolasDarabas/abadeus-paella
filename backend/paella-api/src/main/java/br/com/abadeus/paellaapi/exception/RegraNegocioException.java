package br.com.abadeus.paellaapi.exception;

/** Violacao de regra de negocio (HTTP 422). */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
