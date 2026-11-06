package br.com.abadeus.paellaapi.exception;

/** Requisicao sem usuario logado (HTTP 401). */
public class NaoAutenticadoException extends RuntimeException {
    public NaoAutenticadoException() {
        super("Faca login para acessar este recurso.");
    }

    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
