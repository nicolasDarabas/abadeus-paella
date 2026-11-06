package br.com.abadeus.paella.util;

import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import br.com.abadeus.paella.R;

/** Textos e cores dos status de pedido e de evento. */
public final class StatusUi {

    private StatusUi() {
    }

    public static String pedido(String status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case "AGUARDANDO_PAGAMENTO":
                return "Aguardando pagamento";
            case "PAGO":
                return "Pago · pronto p/ retirar";
            case "RETIRADO":
                return "Retirado";
            case "CANCELADO":
                return "Cancelado";
            case "EXPIRADO":
                return "Expirado";
            case "REEMBOLSADO":
                return "Reembolsado";
            default:
                return status;
        }
    }

    public static String evento(String status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case "RASCUNHO":
                return "Rascunho";
            case "VENDAS_ABERTAS":
                return "Vendas abertas";
            case "VENDAS_ENCERRADAS":
                return "Vendas encerradas";
            case "FINALIZADO":
                return "Finalizado";
            default:
                return status;
        }
    }

    public static String metodo(String metodo) {
        if ("PIX".equals(metodo)) {
            return "Pix";
        }
        if ("CARTAO".equals(metodo)) {
            return "Cartão";
        }
        if ("DINHEIRO".equals(metodo)) {
            return "Dinheiro (no caixa)";
        }
        return metodo == null ? "" : metodo;
    }

    /** Pinta um TextView em formato de "etiqueta" conforme o status. */
    public static void pintar(TextView v, String status) {
        int fundo;
        int texto;
        if ("PAGO".equals(status) || "VENDAS_ABERTAS".equals(status)) {
            fundo = R.color.oliva_claro;
            texto = R.color.oliva;
        } else if ("RETIRADO".equals(status) || "FINALIZADO".equals(status)) {
            fundo = R.color.areia_escura;
            texto = R.color.grafite;
        } else if ("AGUARDANDO_PAGAMENTO".equals(status) || "RASCUNHO".equals(status)) {
            fundo = R.color.acafrao_claro;
            texto = R.color.terracota_escuro;
        } else {
            fundo = R.color.terracota_claro;
            texto = R.color.terracota_escuro;
        }
        v.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(v.getContext(), fundo)));
        v.setTextColor(ContextCompat.getColor(v.getContext(), texto));
    }
}
