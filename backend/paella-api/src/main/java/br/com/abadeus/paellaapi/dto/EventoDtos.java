package br.com.abadeus.paellaapi.dto;

import br.com.abadeus.paellaapi.domain.entity.Evento;
import br.com.abadeus.paellaapi.domain.entity.Produto;
import br.com.abadeus.paellaapi.domain.enums.StatusEvento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class EventoDtos {

    private EventoDtos() {
    }

    // ---------- Evento ----------

    public record EventoRequest(
            @NotBlank(message = "Informe o titulo") @Size(max = 150) String titulo,
            @Size(max = 2000) String descricao,
            @NotBlank(message = "Informe o local") String local,
            @NotNull(message = "Informe a data do evento") LocalDateTime dataEvento,
            LocalDateTime retiradaInicio,
            LocalDateTime retiradaFim,
            @Size(max = 200) String contato,
            @Size(max = 2000) String regras) {
    }

    public record StatusEventoRequest(@NotNull(message = "Informe o status") StatusEvento status) {
    }

    public record EventoResponse(Long id, String titulo, String descricao, String local, LocalDateTime dataEvento,
                                 LocalDateTime retiradaInicio, LocalDateTime retiradaFim, String contato,
                                 String regras, StatusEvento status) {
        public static EventoResponse de(Evento e) {
            return new EventoResponse(e.getId(), e.getTitulo(), e.getDescricao(), e.getLocal(), e.getDataEvento(),
                    e.getRetiradaInicio(), e.getRetiradaFim(), e.getContato(), e.getRegras(), e.getStatus());
        }
    }

    // ---------- Produto ----------

    public record ProdutoRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
            @Size(max = 1000) String descricao,
            @NotNull(message = "Informe o preco") @DecimalMin(value = "0.00", message = "Preco invalido") BigDecimal preco,
            @Min(value = 0, message = "Limite invalido") Integer limiteEstoque,
            Boolean ativo) {
    }

    public record ProdutoResponse(Long id, Long eventoId, String nome, String descricao, BigDecimal preco,
                                  Integer limiteEstoque, boolean ativo) {
        public static ProdutoResponse de(Produto p) {
            return new ProdutoResponse(p.getId(), p.getEvento().getId(), p.getNome(), p.getDescricao(), p.getPreco(),
                    p.getLimiteEstoque(), p.isAtivo());
        }
    }

    /**
     * Item do cardapio publico: preco ja considerando o lote vigente.
     * disponivel = null significa sem limite de estoque.
     */
    public record CardapioItemResponse(Long produtoId, String nome, String descricao, BigDecimal precoAtual,
                                       Long loteId, String loteNome, Integer disponivel, boolean esgotado) {
    }

    public record CardapioResponse(EventoResponse evento, boolean vendasAbertas, List<CardapioItemResponse> itens) {
    }

    // ---------- Lote ----------

    public record LoteRequest(
            @NotBlank(message = "Informe o nome do lote") String nome,
            @NotNull(message = "Informe o inicio das vendas") LocalDateTime vendaInicio,
            @NotNull(message = "Informe o fim das vendas") LocalDateTime vendaFim,
            Boolean ativo) {
    }

    public record LoteProdutoRequest(
            @NotNull(message = "Informe o produto") Long produtoId,
            @NotNull(message = "Informe o preco do lote") @DecimalMin(value = "0.00") BigDecimal precoLote,
            @Min(value = 0) Integer quantidade) {
    }

    public record LoteProdutoResponse(Long id, Long produtoId, String produtoNome, BigDecimal precoLote,
                                      Integer quantidade, long vendidos) {
    }

    public record LoteResponse(Long id, Long eventoId, String nome, LocalDateTime vendaInicio,
                               LocalDateTime vendaFim, boolean ativo, boolean vigente,
                               List<LoteProdutoResponse> produtos) {
    }
}
