package br.com.abadeus.paellaapi.exception;

/** Registro nao encontrado (HTTP 404). */
public class NaoEncontradoException extends RuntimeException {
    public NaoEncontradoException(String entidade, Object id) {
        super(entidade + " nao encontrado(a): " + id);
    }

    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
