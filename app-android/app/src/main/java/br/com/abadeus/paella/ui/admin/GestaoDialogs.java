package br.com.abadeus.paella.ui.admin;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import br.com.abadeus.paella.data.api.ApiClient;
import br.com.abadeus.paella.data.api.Modelos;
import br.com.abadeus.paella.data.api.Resposta;
import br.com.abadeus.paella.util.Formato;
import okhttp3.ResponseBody;

/**
 * Telas de gestao do administrador (dialogos):
 * evento, produtos, lotes, cadastro de voluntario e exportacao de relatorios CSV.
 */
public final class GestaoDialogs {

    private GestaoDialogs() {
    }

    // =================================================================
    // Evento
    // =================================================================

    public static void editarEvento(Fragment f, Modelos.Evento ev, Runnable aoSalvar) {
        Context c = f.requireContext();
        Formulario form = new Formulario(c);
        TextInputEditText titulo = form.campo("Título", ev.titulo, InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        TextInputEditText local = form.campo("Local", ev.local, InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        TextInputEditText data = form.campo("Data do evento (dd/MM/aaaa HH:mm)", Formato.paraCampo(ev.dataEvento), InputType.TYPE_CLASS_DATETIME);
        TextInputEditText inicio = form.campo("Início da retirada (dd/MM/aaaa HH:mm)", Formato.paraCampo(ev.retiradaInicio), InputType.TYPE_CLASS_DATETIME);
        TextInputEditText fim = form.campo("Fim da retirada (dd/MM/aaaa HH:mm)", Formato.paraCampo(ev.retiradaFim), InputType.TYPE_CLASS_DATETIME);
        TextInputEditText contato = form.campo("Contato (WhatsApp / e-mail)", ev.contato, InputType.TYPE_CLASS_TEXT);
        TextInputEditText regras = form.campo("Regras da retirada", ev.regras, InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        new MaterialAlertDialogBuilder(c)
                .setTitle("Editar evento")
                .setView(form.rolavel())
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (d, w) -> {
                    Modelos.EventoRequest req = Modelos.EventoRequest.de(ev);
                    req.titulo = texto(titulo);
                    req.local = texto(local);
                    req.contato = vazioNulo(texto(contato));
                    req.regras = vazioNulo(texto(regras));
                    String dataIso = Formato.deCampo(texto(data));
                    if (dataIso == null || req.titulo.isEmpty() || req.local.isEmpty()) {
                        erro(c, "Preencha título, local e uma data válida (ex.: 05/12/2026 12:00).");
                        return;
                    }
                    req.dataEvento = dataIso;
                    req.retiradaInicio = Formato.deCampo(texto(inicio));
                    req.retiradaFim = Formato.deCampo(texto(fim));
                    ApiClient.get(c).atualizarEvento(ev.id, req).enqueue(new Resposta<Modelos.Evento>(c) {
                        @Override
                        protected void sucesso(Modelos.Evento e) {
                            aviso("Evento atualizado.");
                            aoSalvar.run();
                        }
                    });
                })
                .show();
    }

    // =================================================================
    // Produtos
    // =================================================================

    public static void produtos(Fragment f, long eventoId, Runnable aoSalvar) {
        Context c = f.requireContext();
        ApiClient.get(c).produtosDoEvento(eventoId).enqueue(new Resposta<List<Modelos.Produto>>(c) {
            @Override
            protected void sucesso(List<Modelos.Produto> lista) {
                if (!f.isAdded()) {
                    return;
                }
                String[] nomes = new String[lista.size()];
                for (int i = 0; i < lista.size(); i++) {
                    Modelos.Produto p = lista.get(i);
                    nomes[i] = p.nome + "\n" + Formato.dinheiro(p.preco)
                            + " · estoque " + (p.limiteEstoque == null ? "sem limite" : p.limiteEstoque)
                            + (p.ativo ? "" : " · INATIVO");
                }
                new MaterialAlertDialogBuilder(c)
                        .setTitle("Produtos · toque para editar")
                        .setItems(nomes, (d, i) -> editarProduto(f, lista.get(i), aoSalvar))
                        .setNegativeButton("Fechar", null)
                        .setPositiveButton("Novo produto", (d, w) -> editarProduto(f, null, aoSalvar))
                        .show();
            }
        });
    }

    /** produto == null cria um novo produto no evento atual. */
    private static void editarProduto(Fragment f, Modelos.Produto p, Runnable aoSalvar) {
        Context c = f.requireContext();
        Formulario form = new Formulario(c);
        TextInputEditText nome = form.campo("Nome", p == null ? "" : p.nome, InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        TextInputEditText desc = form.campo("Descrição", p == null ? "" : p.descricao, InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        TextInputEditText preco = form.campo("Preço (ex.: 45.00)", p == null ? "" : String.valueOf(p.preco), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        TextInputEditText limite = form.campo("Limite de estoque (vazio = sem limite)", p == null || p.limiteEstoque == null ? "" : String.valueOf(p.limiteEstoque), InputType.TYPE_CLASS_NUMBER);
        CheckBox ativo = form.opcao("Produto ativo (aparece no cardápio)", p == null || p.ativo);

        new MaterialAlertDialogBuilder(c)
                .setTitle(p == null ? "Novo produto" : "Editar produto")
                .setView(form.rolavel())
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (d, w) -> {
                    Double valor = numero(texto(preco));
                    Integer lim = inteiro(texto(limite));
                    if (texto(nome).isEmpty() || valor == null) {
                        erro(c, "Preencha o nome e um preço válido.");
                        return;
                    }
                    Modelos.ProdutoRequest req = new Modelos.ProdutoRequest(texto(nome), vazioNulo(texto(desc)), valor, lim);
                    req.ativo = ativo.isChecked();
                    Resposta<Modelos.Produto> cb = new Resposta<Modelos.Produto>(c) {
                        @Override
                        protected void sucesso(Modelos.Produto prod) {
                            aviso("Produto salvo: " + prod.nome);
                            aoSalvar.run();
                        }
                    };
                    if (p == null) {
                        long eventoId = br.com.abadeus.paella.data.Sessao.getEventoAdmin(c);
                        ApiClient.get(c).criarProduto(eventoId, req).enqueue(cb);
                    } else {
                        ApiClient.get(c).atualizarProduto(p.id, req).enqueue(cb);
                    }
                })
                .show();
    }

    // =================================================================
    // Lotes
    // =================================================================

    public static void lotes(Fragment f, long eventoId, Runnable aoSalvar) {
        Context c = f.requireContext();
        ApiClient.get(c).lotes(eventoId).enqueue(new Resposta<List<Modelos.Lote>>(c) {
            @Override
            protected void sucesso(List<Modelos.Lote> lista) {
                if (!f.isAdded()) {
                    return;
                }
                String[] nomes = new String[lista.size()];
                for (int i = 0; i < lista.size(); i++) {
                    Modelos.Lote l = lista.get(i);
                    StringBuilder sb = new StringBuilder(l.nome)
                            .append(l.vigente ? " · VIGENTE" : (l.ativo ? "" : " · INATIVO"))
                            .append("\n").append(Formato.paraCampo(l.vendaInicio))
                            .append(" até ").append(Formato.paraCampo(l.vendaFim));
                    for (Modelos.LoteProduto lp : l.produtos) {
                        sb.append("\n• ").append(lp.produtoNome).append(": ").append(Formato.dinheiro(lp.precoLote))
                                .append(lp.quantidade == null ? "" : " (" + lp.vendidos + "/" + lp.quantidade + ")");
                    }
                    nomes[i] = sb.toString();
                }
                new MaterialAlertDialogBuilder(c)
                        .setTitle(lista.isEmpty() ? "Nenhum lote cadastrado" : "Lotes · toque para incluir produto")
                        .setItems(nomes, (d, i) -> escolherProdutoParaLote(f, eventoId, lista.get(i), aoSalvar))
                        .setNegativeButton("Fechar", null)
                        .setPositiveButton("Novo lote", (d, w) -> novoLote(f, eventoId, aoSalvar))
                        .show();
            }
        });
    }

    private static void novoLote(Fragment f, long eventoId, Runnable aoSalvar) {
        Context c = f.requireContext();
        Formulario form = new Formulario(c);
        TextInputEditText nome = form.campo("Nome do lote (ex.: 2º Lote)", "", InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        TextInputEditText inicio = form.campo("Início das vendas (dd/MM/aaaa HH:mm)", "", InputType.TYPE_CLASS_DATETIME);
        TextInputEditText fim = form.campo("Fim das vendas (dd/MM/aaaa HH:mm)", "", InputType.TYPE_CLASS_DATETIME);

        new MaterialAlertDialogBuilder(c)
                .setTitle("Novo lote")
                .setView(form.rolavel())
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Criar", (d, w) -> {
                    String ini = Formato.deCampo(texto(inicio));
                    String fi = Formato.deCampo(texto(fim));
                    if (texto(nome).isEmpty() || ini == null || fi == null) {
                        erro(c, "Preencha o nome e as datas no formato 05/12/2026 12:00.");
                        return;
                    }
                    ApiClient.get(c).criarLote(eventoId, new Modelos.LoteRequest(texto(nome), ini, fi))
                            .enqueue(new Resposta<Modelos.Lote>(c) {
                                @Override
                                protected void sucesso(Modelos.Lote l) {
                                    aviso("Lote criado. Agora inclua os produtos com o preço do lote.");
                                    aoSalvar.run();
                                    if (f.isAdded()) {
                                        escolherProdutoParaLote(f, eventoId, l, aoSalvar);
                                    }
                                }
                            });
                })
                .show();
    }

    private static void escolherProdutoParaLote(Fragment f, long eventoId, Modelos.Lote lote, Runnable aoSalvar) {
        Context c = f.requireContext();
        ApiClient.get(c).produtosDoEvento(eventoId).enqueue(new Resposta<List<Modelos.Produto>>(c) {
            @Override
            protected void sucesso(List<Modelos.Produto> lista) {
                if (!f.isAdded()) {
                    return;
                }
                String[] nomes = new String[lista.size()];
                for (int i = 0; i < lista.size(); i++) {
                    nomes[i] = lista.get(i).nome + " (" + Formato.dinheiro(lista.get(i).preco) + ")";
                }
                new MaterialAlertDialogBuilder(c)
                        .setTitle("Produto no " + lote.nome)
                        .setItems(nomes, (d, i) -> precoNoLote(f, lote, lista.get(i), aoSalvar))
                        .setNegativeButton("Fechar", null)
                        .show();
            }
        });
    }

    private static void precoNoLote(Fragment f, Modelos.Lote lote, Modelos.Produto p, Runnable aoSalvar) {
        Context c = f.requireContext();
        Formulario form = new Formulario(c);
        TextInputEditText preco = form.campo("Preço no lote (ex.: 40.00)", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        TextInputEditText qtd = form.campo("Quantidade no lote (vazio = sem limite)", "", InputType.TYPE_CLASS_NUMBER);

        new MaterialAlertDialogBuilder(c)
                .setTitle(p.nome)
                .setView(form.rolavel())
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (d, w) -> {
                    Double valor = numero(texto(preco));
                    if (valor == null) {
                        erro(c, "Informe um preço válido.");
                        return;
                    }
                    ApiClient.get(c).definirProdutoNoLote(lote.id,
                                    new Modelos.LoteProdutoRequest(p.id, valor, inteiro(texto(qtd))))
                            .enqueue(new Resposta<Modelos.Lote>(c) {
                                @Override
                                protected void sucesso(Modelos.Lote l) {
                                    aviso(p.nome + " incluído no " + l.nome + ".");
                                    aoSalvar.run();
                                }
                            });
                })
                .show();
    }

    // =================================================================
    // Usuarios
    // =================================================================

    public static void novoVoluntario(Fragment f) {
        Context c = f.requireContext();
        Formulario form = new Formulario(c);
        TextInputEditText nome = form.campo("Nome", "", InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        TextInputEditText email = form.campo("E-mail (será o login)", "", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        TextInputEditText senha = form.campo("Senha (mín. 6 caracteres)", "", InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        CheckBox admin = form.opcao("Cadastrar como administrador (acesso total)", false);

        new MaterialAlertDialogBuilder(c)
                .setTitle("Cadastrar equipe")
                .setMessage("O voluntário acessa somente a tela de retirada.")
                .setView(form.rolavel())
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Cadastrar", (d, w) -> {
                    Modelos.NovoUsuarioRequest req = new Modelos.NovoUsuarioRequest(texto(nome), texto(email),
                            texto(senha), admin.isChecked() ? "ADMINISTRADOR" : "VOLUNTARIO");
                    ApiClient.get(c).criarUsuario(req).enqueue(new Resposta<Modelos.Usuario>(c) {
                        @Override
                        protected void sucesso(Modelos.Usuario u) {
                            new MaterialAlertDialogBuilder(c)
                                    .setTitle("Usuário cadastrado")
                                    .setMessage(u.nome + "\nLogin: " + u.login + "\nPerfil: " + u.perfil)
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });
                })
                .show();
    }

    // =================================================================
    // Relatorios CSV
    // =================================================================

    /** tipo = "pedidos" ou "arrecadacao". Baixa o CSV e abre o "Compartilhar" do Android. */
    public static void exportarCsv(Fragment f, long eventoId, String tipo) {
        Context c = f.requireContext();
        ApiClient.get(c).relatorioCsv(tipo, eventoId).enqueue(new Resposta<ResponseBody>(c) {
            @Override
            protected void sucesso(ResponseBody corpo) {
                try {
                    File pasta = new File(c.getCacheDir(), "relatorios");
                    if (!pasta.exists() && !pasta.mkdirs()) {
                        throw new IllegalStateException("Sem pasta para salvar");
                    }
                    File arquivo = new File(pasta, "abadeus-" + tipo + "-evento-" + eventoId + ".csv");
                    try (InputStream in = corpo.byteStream(); OutputStream out = new FileOutputStream(arquivo)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            out.write(buf, 0, n);
                        }
                    }
                    Uri uri = FileProvider.getUriForFile(c, c.getPackageName() + ".arquivos", arquivo);
                    Intent enviar = new Intent(Intent.ACTION_SEND)
                            .setType("text/csv")
                            .putExtra(Intent.EXTRA_STREAM, uri)
                            .putExtra(Intent.EXTRA_SUBJECT, "Relatório ABADEUS - " + tipo)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    c.startActivity(Intent.createChooser(enviar, "Salvar ou enviar relatório"));
                } catch (Exception e) {
                    aviso("Não foi possível salvar o relatório: " + e.getMessage());
                }
            }
        });
    }

    // =================================================================
    // Utilitarios
    // =================================================================

    /** Monta formularios simples com campos do Material Design. */
    static final class Formulario {
        private final Context c;
        private final LinearLayout raiz;

        Formulario(Context c) {
            this.c = c;
            raiz = new LinearLayout(c);
            raiz.setOrientation(LinearLayout.VERTICAL);
            int p = dp(20);
            raiz.setPadding(p, dp(8), p, 0);
        }

        TextInputEditText campo(String rotulo, String valor, int tipo) {
            TextInputLayout caixa = new TextInputLayout(c);
            caixa.setHint(rotulo);
            TextInputEditText e = new TextInputEditText(caixa.getContext());
            int base = (tipo & InputType.TYPE_MASK_CLASS) == 0 ? InputType.TYPE_CLASS_TEXT : 0;
            e.setInputType(base | tipo);
            e.setText(valor == null ? "" : valor);
            caixa.addView(e);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            raiz.addView(caixa, lp);
            return e;
        }

        CheckBox opcao(String texto, boolean marcado) {
            CheckBox cb = new CheckBox(c);
            cb.setText(texto);
            cb.setChecked(marcado);
            raiz.addView(cb);
            return cb;
        }

        android.view.View rolavel() {
            android.widget.ScrollView sv = new android.widget.ScrollView(c);
            sv.addView(raiz);
            return sv;
        }

        private int dp(int v) {
            return (int) (v * c.getResources().getDisplayMetrics().density);
        }
    }

    private static String texto(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private static String vazioNulo(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private static Double numero(String s) {
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer inteiro(String s) {
        try {
            return s.isEmpty() ? null : Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static void erro(Context c, String mensagem) {
        new MaterialAlertDialogBuilder(c).setMessage(mensagem).setPositiveButton("OK", null).show();
    }
}
