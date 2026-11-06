package br.com.abadeus.paellaapi.service;

import java.security.SecureRandom;

/** Gera codigos legiveis (sem letras/numeros que se confundem, como O/0 e I/1). */
public final class CodigoGenerator {

    private static final char[] ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodigoGenerator() {
    }

    public static String gerar(int tamanho) {
        StringBuilder sb = new StringBuilder(tamanho);
        for (int i = 0; i < tamanho; i++) {
            sb.append(ALFABETO[RANDOM.nextInt(ALFABETO.length)]);
        }
        return sb.toString();
    }
}
