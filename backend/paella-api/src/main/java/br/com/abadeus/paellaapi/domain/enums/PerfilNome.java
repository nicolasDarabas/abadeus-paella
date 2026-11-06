package br.com.abadeus.paellaapi.domain.enums;

/**
 * Perfis do sistema.
 * ADMINISTRADOR: configura evento, produtos, lotes, usuarios e relatorios.
 * COMPRADOR: compra e acompanha os proprios pedidos.
 * VOLUNTARIO: equipe de entrega, apenas valida retiradas.
 */
public enum PerfilNome {
    ADMINISTRADOR,
    COMPRADOR,
    VOLUNTARIO
}
