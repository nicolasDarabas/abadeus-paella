package br.com.abadeus.paella.data.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Objetos trocados com a API (JSON). Os nomes dos campos sao iguais aos do servidor.
 */
public final class Modelos {

    private Modelos() {
    }

    // ---------------- Autenticacao ----------------

    public static class LoginRequest {
        public String login;
        public String senha;

        public LoginRequest(String login, String senha) {
            this.login = login;
            this.senha = senha;
        }
    }

    public static class RegistroRequest {
        public String nome;
        public String email;
        public String telefone;
        public String senha;

        public RegistroRequest(String nome, String email, String telefone, String senha) {
            this.nome = nome;
            this.email = email;
            this.telefone = telefone;
            this.senha = senha;
        }
    }

    public static class Usuario {
        public Long id;
        public String nome;
        public String login;
        public String email;
        public String telefone;
        public String perfil;
        public boolean ativo;
    }

    public static class TokenResponse {
        public String token;
        public String tipo;
        public long expiraEmSegundos;
        public Usuario usuario;
    }

    // ---------------- Evento e cardapio ----------------

    public static class Evento {
        public Long id;
        public String titulo;
        public String descricao;
        public String local;
        public String dataEvento;
        public String retiradaInicio;
        public String retiradaFim;
        public String contato;
        public String regras;
        public String status;
    }

    /** Edicao do evento pelo administrador (datas no formato ISO: 2026-12-05T12:00:00). */
    public static class EventoRequest {
        public String titulo;
        public String descricao;
        public String local;
        public String dataEvento;
        public String retiradaInicio;
        public String retiradaFim;
        public String contato;
        public String regras;

        public static EventoRequest de(Evento e) {
            EventoRequest r = new EventoRequest();
            r.titulo = e.titulo;
            r.descricao = e.descricao;
            r.local = e.local;
            r.dataEvento = e.dataEvento;
            r.retiradaInicio = e.retiradaInicio;
            r.retiradaFim = e.retiradaFim;
            r.contato = e.contato;
            r.regras = e.regras;
            return r;
        }
    }

    public static class StatusEventoRequest {
        public String status;

        public StatusEventoRequest(String status) {
            this.status = status;
        }
    }

    public static class CardapioItem {
        public Long produtoId;
        public String nome;
        public String descricao;
        public double precoAtual;
        public Long loteId;
        public String loteNome;
        public Integer disponivel;
        public boolean esgotado;
    }

    public static class Cardapio {
        public Evento evento;
        public boolean vendasAbertas;
        public List<CardapioItem> itens = new ArrayList<>();
    }

    public static class ProdutoRequest {
        public String nome;
        public String descricao;
        public double preco;
        public Integer limiteEstoque;
        public Boolean ativo = true;

        public ProdutoRequest(String nome, String descricao, double preco, Integer limiteEstoque) {
            this.nome = nome;
            this.descricao = descricao;
            this.preco = preco;
            this.limiteEstoque = limiteEstoque;
        }
    }

    // ---------------- Lotes ----------------

    public static class LoteRequest {
        public String nome;
        public String vendaInicio;
        public String vendaFim;
        public Boolean ativo = true;

        public LoteRequest(String nome, String vendaInicio, String vendaFim) {
            this.nome = nome;
            this.vendaInicio = vendaInicio;
            this.vendaFim = vendaFim;
        }
    }

    public static class LoteProdutoRequest {
        public Long produtoId;
        public double precoLote;
        public Integer quantidade;

        public LoteProdutoRequest(Long produtoId, double precoLote, Integer quantidade) {
            this.produtoId = produtoId;
            this.precoLote = precoLote;
            this.quantidade = quantidade;
        }
    }

    public static class LoteProduto {
        public Long id;
        public Long produtoId;
        public String produtoNome;
        public double precoLote;
        public Integer quantidade;
        public long vendidos;
    }

    public static class Lote {
        public Long id;
        public Long eventoId;
        public String nome;
        public String vendaInicio;
        public String vendaFim;
        public boolean ativo;
        public boolean vigente;
        public List<LoteProduto> produtos = new ArrayList<>();
    }

    // ---------------- Usuarios, historico e avisos ----------------

    public static class NovoUsuarioRequest {
        public String nome;
        public String email;
        public String telefone;
        public String senha;
        public String perfil;

        public NovoUsuarioRequest(String nome, String email, String senha, String perfil) {
            this.nome = nome;
            this.email = email;
            this.senha = senha;
            this.perfil = perfil;
        }
    }

    public static class Historico {
        public String statusAnterior;
        public String statusNovo;
        public String motivo;
        public String alteradoPor;
        public String alteradoEm;
    }

    public static class Notificacao {
        public Long id;
        public Long pedidoId;
        public String numeroPedido;
        public String tipo;
        public String destinatario;
        public String assunto;
        public String mensagem;
        public String enviadaEm;
        public String status;
    }

    public static class Produto {
        public Long id;
        public Long eventoId;
        public String nome;
        public String descricao;
        public double preco;
        public Integer limiteEstoque;
        public boolean ativo;
    }

    // ---------------- Pedido e pagamento ----------------

    public static class ItemRequest {
        public Long produtoId;
        public Integer quantidade;

        public ItemRequest(Long produtoId, Integer quantidade) {
            this.produtoId = produtoId;
            this.quantidade = quantidade;
        }
    }

    public static class CriarPedidoRequest {
        public Long eventoId;
        public String origem = "APP";
        public List<ItemRequest> itens = new ArrayList<>();
        public String observacao;
    }

    public static class CancelarRequest {
        public String motivo;

        public CancelarRequest(String motivo) {
            this.motivo = motivo;
        }
    }

    public static class PedidoItem {
        public Long produtoId;
        public String produtoNome;
        public String loteNome;
        public int quantidade;
        public double precoUnitario;
        public double subtotal;
    }

    public static class Pagamento {
        public Long id;
        public Long pedidoId;
        public String metodo;
        public double valor;
        public String status;
        public String codigoPix;
        public String idExterno;
        public String confirmadoEm;
        public String confirmadoPor;
        public String criadoEm;
    }

    public static class Pedido {
        public Long id;
        public String numero;
        public Long eventoId;
        public String eventoTitulo;
        public Long usuarioId;
        public String usuarioNome;
        public String origem;
        public String status;
        public double valorTotal;
        public String expiraEm;
        public String criadoEm;
        public String observacao;
        public boolean qrCodeDisponivel;
        public String retiradoEm;
        public List<PedidoItem> itens = new ArrayList<>();
        public List<Pagamento> pagamentos = new ArrayList<>();

        /** Pagamento pendente mais recente (ou null). */
        public Pagamento pagamentoPendente() {
            if (pagamentos == null) {
                return null;
            }
            for (Pagamento p : pagamentos) {
                if ("PENDENTE".equals(p.status)) {
                    return p;
                }
            }
            return null;
        }

        /** Ex.: "2x Paella Valenciana, 1x Sangria". */
        public String resumoItens() {
            StringBuilder sb = new StringBuilder();
            if (itens != null) {
                for (PedidoItem i : itens) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(i.quantidade).append("x ").append(i.produtoNome);
                }
            }
            return sb.toString();
        }
    }

    public static class IniciarPagamentoRequest {
        public String metodo;

        public IniciarPagamentoRequest(String metodo) {
            this.metodo = metodo;
        }
    }

    public static class Comprovante {
        public Long pedidoId;
        public String numero;
        public String codigoRetirada;
        public String qrCodeBase64;
        public String eventoTitulo;
        public String local;
        public String dataEvento;
        public String retiradaInicio;
        public String retiradaFim;
        public String cliente;
        public String status;
        public double valorTotal;
        public List<PedidoItem> itens = new ArrayList<>();
    }

    // ---------------- Retirada ----------------

    public static class ValidarRetiradaRequest {
        public String codigo;
        public String metodo;

        public ValidarRetiradaRequest(String codigo, String metodo) {
            this.codigo = codigo;
            this.metodo = metodo;
        }
    }

    public static class ConsultaRetirada {
        public Long pedidoId;
        public String numero;
        public String cliente;
        public String status;
        public boolean podeRetirar;
        public String mensagem;
        public List<PedidoItem> itens = new ArrayList<>();
    }

    public static class Retirada {
        public Long retiradaId;
        public Long pedidoId;
        public String numeroPedido;
        public String cliente;
        public String validadoPor;
        public String validadoEm;
        public String metodoValidacao;
        public boolean sincronizadoOffline;
        public String comprovanteCodigo;
        public List<PedidoItem> itens = new ArrayList<>();
    }

    public static class PendenteRetirada {
        public Long pedidoId;
        public String numero;
        public String codigoRetirada;
        public String cliente;
        public List<PedidoItem> itens = new ArrayList<>();
    }

    public static class SyncRetiradaItem {
        public String codigo;
        public String validadoEm;

        public SyncRetiradaItem(String codigo, String validadoEm) {
            this.codigo = codigo;
            this.validadoEm = validadoEm;
        }
    }

    public static class SyncRetiradasRequest {
        public List<SyncRetiradaItem> retiradas = new ArrayList<>();
    }

    public static class SyncResultado {
        public String codigo;
        public String resultado;
        public String mensagem;
    }

    // ---------------- Painel ----------------

    public static class ProdutoVendido {
        public Long produtoId;
        public String nome;
        public long vendidos;
        public Integer limiteEstoque;
        public Integer disponivel;
    }

    public static class Resumo {
        public Long eventoId;
        public String titulo;
        public long totalPedidos;
        public Map<String, Long> pedidosPorStatus;
        public double valorArrecadado;
        public double valorAguardandoPagamento;
        public long retirados;
        public long aRetirar;
        public List<ProdutoVendido> produtos = new ArrayList<>();
    }

    // ---------------- Erro padrao da API ----------------

    public static class Erro {
        public int status;
        public String erro;
        public String mensagem;
        public Map<String, String> campos;
    }
}
