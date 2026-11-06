package br.com.abadeus.paellaapi.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato padrao das respostas de erro da API. */
public record ErroResposta(
        int status,
        String erro,
        String mensagem,
        LocalDateTime momento,
        Map<String, String> campos
) {
    public static ErroResposta de(int status, String erro, String mensagem) {
        return new ErroResposta(status, erro, mensagem, LocalDateTime.now(), null);
    }
}
