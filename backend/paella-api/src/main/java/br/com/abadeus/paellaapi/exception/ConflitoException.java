package br.com.abadeus.paellaapi.exception;

/** Conflito de estado, ex.: pedido ja retirado (HTTP 409). */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
