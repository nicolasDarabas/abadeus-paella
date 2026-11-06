package br.com.abadeus.paella.util;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formata dinheiro e datas no padrao brasileiro. */
public final class Formato {

    private static final Locale BR = new Locale("pt", "BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", BR);
    private static final DateTimeFormatter DATA_LONGA = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", BR);
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM", BR);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", BR);

    private Formato() {
    }

    public static String dinheiro(double valor) {
        return NumberFormat.getCurrencyInstance(BR).format(valor);
    }

    private static LocalDateTime ler(String iso) {
        if (iso == null || iso.isEmpty()) {
            return null;
        }
        try {
            String s = iso.length() > 19 ? iso.substring(0, 19) : iso;
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static String dataHora(String iso) {
        LocalDateTime d = ler(iso);
        return d == null ? "" : d.format(DATA_HORA);
    }

    /** Ex.: "Sábado, 05 de dezembro". */
    public static String dataLonga(String iso) {
        LocalDateTime d = ler(iso);
        if (d == null) {
            return "";
        }
        String s = d.format(DATA_LONGA);
        return s.substring(0, 1).toUpperCase(BR) + s.substring(1);
    }

    public static String hora(String iso) {
        LocalDateTime d = ler(iso);
        return d == null ? "" : d.format(HORA);
    }

    private static final DateTimeFormatter CAMPO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", BR);

    /** Para preencher campos de edicao: "05/12/2026 12:00". */
    public static String paraCampo(String iso) {
        LocalDateTime d = ler(iso);
        return d == null ? "" : d.format(CAMPO);
    }

    /** Converte "05/12/2026 12:00" para ISO ("2026-12-05T12:00:00"). Retorna null se invalido. */
    public static String deCampo(String texto) {
        try {
            return LocalDateTime.parse(texto.trim(), CAMPO).withSecond(0).toString() + ":00";
        } catch (Exception e) {
            return null;
        }
    }

    /** Ex.: "05/12 das 11:00 às 15:00". */
    public static String janela(String inicio, String fim) {
        LocalDateTime i = ler(inicio);
        LocalDateTime f = ler(fim);
        if (i == null) {
            return "";
        }
        String s = i.format(DIA_MES) + " das " + i.format(HORA);
        if (f != null) {
            s += " às " + f.format(HORA);
        }
        return s;
    }
}
