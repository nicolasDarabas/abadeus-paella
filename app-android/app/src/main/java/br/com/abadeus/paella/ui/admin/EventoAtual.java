package br.com.abadeus.paella.ui.admin;

import android.content.Context;

import java.util.List;

import br.com.abadeus.paella.data.Sessao;
import br.com.abadeus.paella.data.api.ApiClient;
import br.com.abadeus.paella.data.api.Modelos;
import br.com.abadeus.paella.data.api.Resposta;

/** Descobre qual evento o administrador esta gerenciando. */
public final class EventoAtual {

    public interface Pronto {
        void evento(Modelos.Evento evento, List<Modelos.Evento> todos);
    }

    private EventoAtual() {
    }

    public static void obter(Context c, Pronto pronto) {
        ApiClient.get(c).eventos().enqueue(new Resposta<List<Modelos.Evento>>(c) {
            @Override
            protected void sucesso(List<Modelos.Evento> eventos) {
                if (eventos == null || eventos.isEmpty()) {
                    pronto.evento(null, eventos);
                    return;
                }
                long salvo = Sessao.getEventoAdmin(c);
                Modelos.Evento escolhido = null;
                for (Modelos.Evento e : eventos) {
                    if (e.id == salvo) {
                        escolhido = e;
                    }
                }
                if (escolhido == null) {
                    for (Modelos.Evento e : eventos) {
                        if ("VENDAS_ABERTAS".equals(e.status)) {
                            escolhido = e;
                            break;
                        }
                    }
                }
                if (escolhido == null) {
                    escolhido = eventos.get(0);
                }
                Sessao.setEventoAdmin(c, escolhido.id);
                pronto.evento(escolhido, eventos);
            }
        });
    }
}
