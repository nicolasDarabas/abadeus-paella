package br.com.abadeus.paella.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.List;

import br.com.abadeus.paella.R;
import br.com.abadeus.paella.data.BancoLocal;
import br.com.abadeus.paella.data.Sessao;
import br.com.abadeus.paella.data.api.ApiClient;
import br.com.abadeus.paella.data.api.Modelos;
import br.com.abadeus.paella.data.api.Resposta;
import br.com.abadeus.paella.util.Formato;

/**
 * RETIRADA no dia do evento.
 * Online: le o QR -> consulta o servidor -> confirma a entrega (retirada unica).
 * Contingencia: valida pela lista salva no banco local e sincroniza depois.
 */
public class RetiradaFragment extends Fragment {

    private MaterialSwitch switchOffline;
    private TextInputEditText editCodigo;
    private MaterialCardView cardResultado;
    private ImageView resIcone;
    private TextView resTitulo;
    private TextView resPedido;
    private TextView resItens;
    private TextView resDetalhe;
    private TextView textoLocal;
    private MaterialButton botaoBaixar;
    private MaterialButton botaoSincronizar;

    /** Abre a camera (biblioteca ZXing) e devolve o texto do QR Code. */
    private final ActivityResultLauncher<ScanOptions> leitor = registerForActivityResult(new ScanContract(), r -> {
        if (r.getContents() != null) {
            validar(r.getContents(), "QR_CODE");
        }
    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_retirada, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        if (Sessao.isVoluntario(requireContext())) {
            // o voluntario ja tem a barra superior com o titulo: evita cabecalho duplicado
            v.findViewById(R.id.tituloRetirada).setVisibility(View.GONE);
            v.findViewById(R.id.subtituloRetirada).setVisibility(View.GONE);
        }
        switchOffline = v.findViewById(R.id.switchOffline);
        editCodigo = v.findViewById(R.id.editCodigo);
        cardResultado = v.findViewById(R.id.cardResultado);
        resIcone = v.findViewById(R.id.resIcone);
        resTitulo = v.findViewById(R.id.resTitulo);
        resPedido = v.findViewById(R.id.resPedido);
        resItens = v.findViewById(R.id.resItens);
        resDetalhe = v.findViewById(R.id.resDetalhe);
        textoLocal = v.findViewById(R.id.textoLocal);
        botaoBaixar = v.findViewById(R.id.botaoBaixar);
        botaoSincronizar = v.findViewById(R.id.botaoSincronizar);

        switchOffline.setChecked(Sessao.isModoOffline(requireContext()));
        switchOffline.setOnCheckedChangeListener((b, ativo) -> {
            Sessao.setModoOffline(requireContext(), ativo);
            atualizarContadores();
        });

        v.findViewById(R.id.botaoLerQr).setOnClickListener(b -> abrirLeitor());
        v.findViewById(R.id.botaoValidarManual).setOnClickListener(b -> validarDigitado());
        editCodigo.setOnEditorActionListener((t, acao, e) -> {
            if (acao == EditorInfo.IME_ACTION_DONE) {
                validarDigitado();
                return true;
            }
            return false;
        });
        botaoBaixar.setOnClickListener(b -> baixarLista());
        botaoSincronizar.setOnClickListener(b -> sincronizar());
        atualizarContadores();
    }

    private void abrirLeitor() {
        ScanOptions opcoes = new ScanOptions();
        opcoes.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        opcoes.setPrompt("Aponte para o QR Code do pedido");
        opcoes.setBeepEnabled(true);
        opcoes.setOrientationLocked(false);
        leitor.launch(opcoes);
    }

    private void validarDigitado() {
        String codigo = editCodigo.getText() == null ? "" : editCodigo.getText().toString().trim();
        if (!codigo.isEmpty()) {
            validar(codigo, "CODIGO_MANUAL");
        }
    }

    private void validar(String codigo, String metodo) {
        if (Sessao.isModoOffline(requireContext())) {
            validarOffline(codigo);
        } else {
            consultarOnline(codigo, metodo);
        }
    }

    // =================================================================
    // Online
    // =================================================================

    private void consultarOnline(String codigo, String metodo) {
        ApiClient.get(requireContext()).consultarRetirada(codigo)
                .enqueue(new Resposta<Modelos.ConsultaRetirada>(requireContext()) {
                    @Override
                    protected void sucesso(Modelos.ConsultaRetirada c) {
                        if (!isAdded()) {
                            return;
                        }
                        String itens = itens(c.itens);
                        if (!c.podeRetirar) {
                            mostrarErro("NÃO ENTREGAR", c.numero + " · " + c.cliente, itens, c.mensagem);
                            return;
                        }
                        new MaterialAlertDialogBuilder(requireContext())
                                .setIcon(R.drawable.ic_check)
                                .setTitle("Entregar pedido " + c.numero + "?")
                                .setMessage("Cliente: " + c.cliente + "\n\n" + itens)
                                .setNegativeButton("Cancelar", null)
                                .setPositiveButton("Confirmar entrega", (d, w) -> confirmarOnline(codigo, metodo))
                                .show();
                    }

                    @Override
                    protected void falha(int status, String mensagem) {
                        mostrarErro("Código inválido", codigo, "", mensagem);
                    }

                    @Override
                    protected void semConexao() {
                        sugerirContingencia(codigo);
                    }
                });
    }

    private void confirmarOnline(String codigo, String metodo) {
        ApiClient.get(requireContext()).validarRetirada(new Modelos.ValidarRetiradaRequest(codigo, metodo))
                .enqueue(new Resposta<Modelos.Retirada>(requireContext()) {
                    @Override
                    protected void sucesso(Modelos.Retirada r) {
                        if (!isAdded()) {
                            return;
                        }
                        editCodigo.setText("");
                        mostrarOk("ENTREGUE", r.numeroPedido + " · " + r.cliente, itens(r.itens),
                                "Comprovante " + r.comprovanteCodigo + " · " + Formato.dataHora(r.validadoEm));
                    }

                    @Override
                    protected void falha(int status, String mensagem) {
                        // 409 = ja retirado (regra da retirada unica)
                        mostrarErro(status == 409 ? "JÁ RETIRADO" : "NÃO ENTREGAR", codigo, "", mensagem);
                    }

                    @Override
                    protected void semConexao() {
                        sugerirContingencia(codigo);
                    }
                });
    }

    private void sugerirContingencia(String codigo) {
        if (!isAdded()) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sem conexão com o servidor")
                .setMessage("Deseja ativar o modo contingência e validar pela lista salva no aparelho?")
                .setNegativeButton("Não", null)
                .setPositiveButton("Ativar", (d, w) -> {
                    switchOffline.setChecked(true);
                    validarOffline(codigo);
                })
                .show();
    }

    // =================================================================
    // Contingencia (banco local)
    // =================================================================

    private void validarOffline(String codigo) {
        BancoLocal.ResultadoOffline r = BancoLocal.get(requireContext()).validarOffline(codigo);
        if (r == null) {
            mostrarErro("NÃO ENCONTRADO", codigo, "",
                    "Este código não está na lista local. Confira se o pedido está pago ou baixe a lista novamente.");
        } else if (r.jaRetirado) {
            mostrarErro("JÁ RETIRADO", r.numero + " · " + r.cliente, r.itens,
                    "Entregue em " + Formato.dataHora(r.validadoEm) + " (modo contingência).");
        } else {
            editCodigo.setText("");
            mostrarOk("ENTREGUE (offline)", r.numero + " · " + r.cliente, r.itens,
                    "Será enviado ao servidor quando você tocar em Sincronizar.");
        }
        atualizarContadores();
    }

    private void baixarLista() {
        long eventoId = Sessao.getEventoAdmin(requireContext());
        if (eventoId <= 0) {
            // Voluntario (ou admin que ainda nao abriu o Painel): descobre o evento atual
            botaoBaixar.setEnabled(false);
            EventoAtual.obter(requireContext(), (ev, todos) -> {
                if (!isAdded()) {
                    return;
                }
                botaoBaixar.setEnabled(true);
                if (ev == null) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setMessage("Nenhum evento encontrado.")
                            .setPositiveButton("OK", null).show();
                } else {
                    baixarLista();
                }
            });
            return;
        }
        botaoBaixar.setEnabled(false);
        ApiClient.get(requireContext()).retiradasPendentes(eventoId)
                .enqueue(new Resposta<List<Modelos.PendenteRetirada>>(requireContext()) {
                    @Override
                    protected void sucesso(List<Modelos.PendenteRetirada> lista) {
                        int n = BancoLocal.get(context).salvarPendentes(eventoId, lista);
                        aviso(n + " pedido(s) pago(s) salvos no aparelho.");
                    }

                    @Override
                    protected void fim() {
                        if (isAdded()) {
                            botaoBaixar.setEnabled(true);
                            atualizarContadores();
                        }
                    }
                });
    }

    private void sincronizar() {
        List<Modelos.SyncRetiradaItem> itens = BancoLocal.get(requireContext()).retiradasParaEnviar();
        if (itens.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setMessage("Nenhuma retirada offline aguardando envio.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        Modelos.SyncRetiradasRequest req = new Modelos.SyncRetiradasRequest();
        req.retiradas.addAll(itens);
        botaoSincronizar.setEnabled(false);
        ApiClient.get(requireContext()).sincronizarRetiradas(req)
                .enqueue(new Resposta<List<Modelos.SyncResultado>>(requireContext()) {
                    @Override
                    protected void sucesso(List<Modelos.SyncResultado> resultados) {
                        int ok = 0;
                        int jaRetirado = 0;
                        int invalidos = 0;
                        StringBuilder problemas = new StringBuilder();
                        for (Modelos.SyncResultado r : resultados) {
                            if ("OK".equals(r.resultado)) {
                                ok++;
                                BancoLocal.get(context).marcarSincronizado(r.codigo);
                            } else if ("JA_RETIRADO".equals(r.resultado)) {
                                jaRetirado++;
                                BancoLocal.get(context).marcarSincronizado(r.codigo);
                                problemas.append("\n• ").append(r.codigo).append(": ").append(r.mensagem);
                            } else {
                                invalidos++;
                                BancoLocal.get(context).marcarSincronizado(r.codigo);
                                problemas.append("\n• ").append(r.codigo).append(": ").append(r.mensagem);
                            }
                        }
                        if (!isAdded()) {
                            return;
                        }
                        Sessao.setModoOffline(context, false);
                        switchOffline.setChecked(false);
                        new MaterialAlertDialogBuilder(requireContext())
                                .setIcon(R.drawable.ic_sync)
                                .setTitle("Sincronização concluída")
                                .setMessage(ok + " retirada(s) registrada(s) no servidor."
                                        + (jaRetirado + invalidos > 0
                                        ? "\n\nAtenção (" + (jaRetirado + invalidos) + "):" + problemas : ""))
                                .setPositiveButton("OK", null)
                                .show();
                    }

                    @Override
                    protected void fim() {
                        if (isAdded()) {
                            botaoSincronizar.setEnabled(true);
                            atualizarContadores();
                        }
                    }
                });
    }

    private void atualizarContadores() {
        BancoLocal banco = BancoLocal.get(requireContext());
        int naLista = banco.contarPendentesNaLista();
        int aEnviar = banco.contarAguardandoEnvio();
        textoLocal.setText(naLista + " pedido(s) pago(s) na lista local\n"
                + aEnviar + " retirada(s) aguardando envio ao servidor");
        botaoSincronizar.setText(aEnviar > 0 ? "Sincronizar (" + aEnviar + ")" : "Sincronizar");
    }

    // =================================================================
    // Cartao de resultado
    // =================================================================

    private void mostrarOk(String titulo, String pedido, String itens, String detalhe) {
        mostrarResultado(true, titulo, pedido, itens, detalhe);
    }

    private void mostrarErro(String titulo, String pedido, String itens, String detalhe) {
        mostrarResultado(false, titulo, pedido, itens, detalhe);
    }

    private void mostrarResultado(boolean ok, String titulo, String pedido, String itens, String detalhe) {
        if (!isAdded()) {
            return;
        }
        cardResultado.setVisibility(View.VISIBLE);
        int fundo = ok ? R.color.oliva_claro : R.color.terracota_claro;
        int cor = ok ? R.color.oliva : R.color.terracota;
        cardResultado.setCardBackgroundColor(ContextCompat.getColor(requireContext(), fundo));
        resIcone.setImageResource(ok ? R.drawable.ic_check : R.drawable.ic_erro);
        resIcone.setColorFilter(ContextCompat.getColor(requireContext(), cor));
        resTitulo.setText(titulo);
        resTitulo.setTextColor(ContextCompat.getColor(requireContext(), cor));
        resPedido.setText(pedido);
        resItens.setText(itens);
        resItens.setVisibility(itens == null || itens.isEmpty() ? View.GONE : View.VISIBLE);
        resDetalhe.setText(detalhe);
    }

    private static String itens(List<Modelos.PedidoItem> lista) {
        StringBuilder sb = new StringBuilder();
        if (lista != null) {
            for (Modelos.PedidoItem i : lista) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(i.quantidade).append("x ").append(i.produtoNome);
            }
        }
        return sb.toString();
    }
}
