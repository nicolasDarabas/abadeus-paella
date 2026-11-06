package br.com.abadeus.paella.ui.comprador;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.com.abadeus.paella.R;
import br.com.abadeus.paella.data.Sessao;
import br.com.abadeus.paella.data.api.ApiClient;
import br.com.abadeus.paella.data.api.Modelos;
import br.com.abadeus.paella.data.api.Resposta;
import br.com.abadeus.paella.util.Formato;
import br.com.abadeus.paella.ui.auth.LoginActivity;

/** Dados do usuario, como funciona e botao sair. */
public class PerfilFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        ((TextView) v.findViewById(R.id.perfilNome)).setText(Sessao.getNome(requireContext()));
        ((TextView) v.findViewById(R.id.perfilEmail)).setText(Sessao.getEmail(requireContext()));
        ((TextView) v.findViewById(R.id.perfilTipo)).setText(
                Sessao.isAdministrador(requireContext()) ? "Administrador" : "Comprador");
        ((TextView) v.findViewById(R.id.perfilServidor)).setText("Servidor: " + Sessao.getServidor(requireContext()));
        v.findViewById(R.id.botaoSair).setOnClickListener(b -> confirmarSaida(this));
        carregarAvisos(v);
    }

    /** Lista os avisos (e-mails simulados) enviados ao comprador. */
    private void carregarAvisos(View v) {
        android.widget.LinearLayout lista = v.findViewById(R.id.listaAvisos);
        View card = v.findViewById(R.id.cardAvisos);
        ApiClient.get(requireContext()).meusAvisos().enqueue(new Resposta<java.util.List<Modelos.Notificacao>>(requireContext()) {
            @Override
            protected void sucesso(java.util.List<Modelos.Notificacao> avisos) {
                if (!isAdded() || avisos.isEmpty()) {
                    return;
                }
                card.setVisibility(View.VISIBLE);
                lista.removeAllViews();
                LayoutInflater inf = LayoutInflater.from(requireContext());
                int max = Math.min(avisos.size(), 10);
                for (int i = 0; i < max; i++) {
                    Modelos.Notificacao n = avisos.get(i);
                    View linha = inf.inflate(R.layout.linha_historico, lista, false);
                    ((TextView) linha.findViewById(R.id.histTitulo)).setText(n.assunto);
                    ((TextView) linha.findViewById(R.id.histDetalhe)).setText(
                            Formato.dataHora(n.enviadaEm) + "\n" + n.mensagem);
                    lista.addView(linha);
                }
            }

            @Override
            protected void semConexao() {
                // sem internet: o cartao de avisos fica oculto
            }
        });
    }

    /** Usado tambem pelo painel do administrador. */
    public static void confirmarSaida(Fragment f) {
        new MaterialAlertDialogBuilder(f.requireContext())
                .setTitle("Sair da conta?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sair", (d, w) -> {
                    Sessao.sair(f.requireContext());
                    Intent i = new Intent(f.requireContext(), LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    f.startActivity(i);
                })
                .show();
    }
}
