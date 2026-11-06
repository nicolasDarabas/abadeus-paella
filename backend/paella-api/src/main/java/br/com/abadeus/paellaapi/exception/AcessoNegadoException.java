package br.com.abadeus.paellaapi.exception;

/** Usuario tentando acessar dado de outro usuario (HTTP 403). */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
