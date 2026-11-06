package br.com.abadeus.paella.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import br.com.abadeus.paella.data.api.Modelos;

/**
 * BANCO LOCAL (SQLite no proprio celular).
 * - Comprador: guarda o cardapio e o QR Code dos pedidos pagos (funciona sem internet).
 * - Administrador: guarda a lista de pedidos pagos para validar retiradas sem internet
 *   (modo contingencia) e depois sincroniza com o servidor.
 */
public class BancoLocal extends SQLiteOpenHelper {

    private static final String NOME = "abadeus_local.db";
    private static final int VERSAO = 2;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static BancoLocal instancia;

    public static synchronized BancoLocal get(Context c) {
        if (instancia == null) {
            instancia = new BancoLocal(c.getApplicationContext());
        }
        return instancia;
    }

    private BancoLocal(Context c) {
        super(c, NOME, null, VERSAO);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE evento (id INTEGER PRIMARY KEY, titulo TEXT, local TEXT, data_evento TEXT, "
                + "retirada_inicio TEXT, retirada_fim TEXT, status TEXT, descricao TEXT, contato TEXT, regras TEXT)");
        db.execSQL("CREATE TABLE cardapio (produto_id INTEGER PRIMARY KEY, evento_id INTEGER, nome TEXT, "
                + "descricao TEXT, preco REAL, lote_nome TEXT, disponivel INTEGER, esgotado INTEGER)");
        db.execSQL("CREATE TABLE comprovante (pedido_id INTEGER PRIMARY KEY, numero TEXT, codigo TEXT, "
                + "qr_base64 TEXT, evento TEXT, local TEXT, retirada_inicio TEXT, retirada_fim TEXT, "
                + "itens TEXT, valor REAL, salvo_em TEXT)");
        db.execSQL("CREATE TABLE retirada_local (codigo TEXT PRIMARY KEY, pedido_id INTEGER, numero TEXT, "
                + "cliente TEXT, itens TEXT, evento_id INTEGER, retirado INTEGER DEFAULT 0, "
                + "validado_em TEXT, sincronizado INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int antiga, int nova) {
        db.execSQL("DROP TABLE IF EXISTS evento");
        db.execSQL("DROP TABLE IF EXISTS cardapio");
        db.execSQL("DROP TABLE IF EXISTS comprovante");
        db.execSQL("DROP TABLE IF EXISTS retirada_local");
        onCreate(db);
    }

    // =====================================================================
    // Cardapio (comprador)
    // =====================================================================

    public void salvarCardapio(Modelos.Cardapio c) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Modelos.Evento e = c.evento;
            ContentValues ev = new ContentValues();
            ev.put("id", e.id);
            ev.put("titulo", e.titulo);
            ev.put("local", e.local);
            ev.put("data_evento", e.dataEvento);
            ev.put("retirada_inicio", e.retiradaInicio);
            ev.put("retirada_fim", e.retiradaFim);
            ev.put("status", e.status);
            ev.put("descricao", e.descricao);
            ev.put("contato", e.contato);
            ev.put("regras", e.regras);
            db.delete("evento", null, null);
            db.insert("evento", null, ev);

            db.delete("cardapio", null, null);
            for (Modelos.CardapioItem i : c.itens) {
                ContentValues v = new ContentValues();
                v.put("produto_id", i.produtoId);
                v.put("evento_id", e.id);
                v.put("nome", i.nome);
                v.put("descricao", i.descricao);
                v.put("preco", i.precoAtual);
                v.put("lote_nome", i.loteNome);
                if (i.disponivel != null) {
                    v.put("disponivel", i.disponivel);
                } else {
                    v.putNull("disponivel");
                }
                v.put("esgotado", i.esgotado ? 1 : 0);
                db.insert("cardapio", null, v);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Cardapio salvo na ultima vez que houve internet (ou null). */
    public Modelos.Cardapio carregarCardapio() {
        SQLiteDatabase db = getReadableDatabase();
        Modelos.Cardapio c = null;
        try (Cursor ce = db.rawQuery("SELECT id, titulo, local, data_evento, retirada_inicio, retirada_fim, status, descricao, contato, regras FROM evento LIMIT 1", null)) {
            if (!ce.moveToFirst()) {
                return null;
            }
            c = new Modelos.Cardapio();
            Modelos.Evento e = new Modelos.Evento();
            e.id = ce.getLong(0);
            e.titulo = ce.getString(1);
            e.local = ce.getString(2);
            e.dataEvento = ce.getString(3);
            e.retiradaInicio = ce.getString(4);
            e.retiradaFim = ce.getString(5);
            e.status = ce.getString(6);
            e.descricao = ce.getString(7);
            e.contato = ce.getString(8);
            e.regras = ce.getString(9);
            c.evento = e;
            c.vendasAbertas = false; // sem internet nao da para comprar
        }
        try (Cursor cur = db.rawQuery("SELECT produto_id, nome, descricao, preco, lote_nome, disponivel, esgotado FROM cardapio ORDER BY nome", null)) {
            while (cur.moveToNext()) {
                Modelos.CardapioItem i = new Modelos.CardapioItem();
                i.produtoId = cur.getLong(0);
                i.nome = cur.getString(1);
                i.descricao = cur.getString(2);
                i.precoAtual = cur.getDouble(3);
                i.loteNome = cur.getString(4);
                i.disponivel = cur.isNull(5) ? null : cur.getInt(5);
                i.esgotado = cur.getInt(6) == 1;
                c.itens.add(i);
            }
        }
        return c;
    }

    // =====================================================================
    // Comprovantes com QR Code (comprador)
    // =====================================================================

    public void salvarComprovante(Modelos.Comprovante c) {
        ContentValues v = new ContentValues();
        v.put("pedido_id", c.pedidoId);
        v.put("numero", c.numero);
        v.put("codigo", c.codigoRetirada);
        v.put("qr_base64", c.qrCodeBase64);
        v.put("evento", c.eventoTitulo);
        v.put("local", c.local);
        v.put("retirada_inicio", c.retiradaInicio);
        v.put("retirada_fim", c.retiradaFim);
        v.put("itens", resumo(c.itens));
        v.put("valor", c.valorTotal);
        v.put("salvo_em", agora());
        getWritableDatabase().insertWithOnConflict("comprovante", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Modelos.Comprovante carregarComprovante(long pedidoId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT numero, codigo, qr_base64, evento, local, retirada_inicio, retirada_fim, valor FROM comprovante WHERE pedido_id = ?",
                new String[]{String.valueOf(pedidoId)})) {
            if (!c.moveToFirst()) {
                return null;
            }
            Modelos.Comprovante r = new Modelos.Comprovante();
            r.pedidoId = pedidoId;
            r.numero = c.getString(0);
            r.codigoRetirada = c.getString(1);
            r.qrCodeBase64 = c.getString(2);
            r.eventoTitulo = c.getString(3);
            r.local = c.getString(4);
            r.retiradaInicio = c.getString(5);
            r.retiradaFim = c.getString(6);
            r.valorTotal = c.getDouble(7);
            return r;
        }
    }

    /** Pedidos com QR salvo, para o comprador abrir mesmo sem internet. */
    public List<Modelos.Pedido> pedidosSalvos() {
        List<Modelos.Pedido> lista = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT pedido_id, numero, evento, valor, salvo_em, itens FROM comprovante ORDER BY salvo_em DESC", null)) {
            while (c.moveToNext()) {
                Modelos.Pedido p = new Modelos.Pedido();
                p.id = c.getLong(0);
                p.numero = c.getString(1);
                p.eventoTitulo = c.getString(2);
                p.valorTotal = c.getDouble(3);
                p.criadoEm = c.getString(4);
                p.status = "PAGO";
                String itens = c.getString(5);
                if (itens != null) {
                    for (String linha : itens.split("\n")) {
                        int x = linha.indexOf("x ");
                        if (x > 0) {
                            Modelos.PedidoItem i = new Modelos.PedidoItem();
                            try {
                                i.quantidade = Integer.parseInt(linha.substring(0, x));
                            } catch (NumberFormatException e) {
                                i.quantidade = 1;
                            }
                            i.produtoNome = linha.substring(x + 2);
                            p.itens.add(i);
                        }
                    }
                }
                lista.add(p);
            }
        }
        return lista;
    }

    // =====================================================================
    // Retiradas em modo contingencia (administrador)
    // =====================================================================

    /** Substitui a lista local pelos pedidos pagos baixados, preservando o que ainda nao foi enviado. */
    public int salvarPendentes(long eventoId, List<Modelos.PendenteRetirada> lista) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("retirada_local", "NOT (retirado = 1 AND sincronizado = 0)", null);
            for (Modelos.PendenteRetirada p : lista) {
                ContentValues v = new ContentValues();
                v.put("codigo", p.codigoRetirada);
                v.put("pedido_id", p.pedidoId);
                v.put("numero", p.numero);
                v.put("cliente", p.cliente);
                v.put("itens", resumo(p.itens));
                v.put("evento_id", eventoId);
                db.insertWithOnConflict("retirada_local", null, v, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return lista.size();
    }

    /** Resultado de uma validacao feita sem internet. */
    public static class ResultadoOffline {
        public boolean ok;
        public boolean jaRetirado;
        public String numero;
        public String cliente;
        public String itens;
        public String validadoEm;
    }

    /** Valida pelo codigo do QR (ABD-...) ou pelo numero do pedido (PAE-...). Null = nao encontrado. */
    public ResultadoOffline validarOffline(String codigoOuNumero) {
        String chave = codigoOuNumero.trim().toUpperCase();
        SQLiteDatabase db = getWritableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT codigo, numero, cliente, itens, retirado, validado_em FROM retirada_local WHERE codigo = ? OR numero = ? LIMIT 1",
                new String[]{chave, chave})) {
            if (!c.moveToFirst()) {
                return null;
            }
            ResultadoOffline r = new ResultadoOffline();
            String codigo = c.getString(0);
            r.numero = c.getString(1);
            r.cliente = c.getString(2);
            r.itens = c.getString(3);
            if (c.getInt(4) == 1) {
                r.jaRetirado = true;
                r.validadoEm = c.getString(5);
                return r;
            }
            r.validadoEm = agora();
            ContentValues v = new ContentValues();
            v.put("retirado", 1);
            v.put("validado_em", r.validadoEm);
            v.put("sincronizado", 0);
            db.update("retirada_local", v, "codigo = ?", new String[]{codigo});
            r.ok = true;
            return r;
        }
    }

    public int contarPendentesNaLista() {
        return contar("SELECT COUNT(*) FROM retirada_local WHERE retirado = 0");
    }

    public int contarAguardandoEnvio() {
        return contar("SELECT COUNT(*) FROM retirada_local WHERE retirado = 1 AND sincronizado = 0");
    }

    public List<Modelos.SyncRetiradaItem> retiradasParaEnviar() {
        List<Modelos.SyncRetiradaItem> lista = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT codigo, validado_em FROM retirada_local WHERE retirado = 1 AND sincronizado = 0", null)) {
            while (c.moveToNext()) {
                lista.add(new Modelos.SyncRetiradaItem(c.getString(0), c.getString(1)));
            }
        }
        return lista;
    }

    public void marcarSincronizado(String codigo) {
        ContentValues v = new ContentValues();
        v.put("sincronizado", 1);
        getWritableDatabase().update("retirada_local", v, "codigo = ?", new String[]{codigo});
    }

    // =====================================================================

    private int contar(String sql) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private static String agora() {
        return LocalDateTime.now().format(ISO);
    }

    private static String resumo(List<Modelos.PedidoItem> itens) {
        StringBuilder sb = new StringBuilder();
        if (itens != null) {
            for (Modelos.PedidoItem i : itens) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(i.quantidade).append("x ").append(i.produtoNome);
            }
        }
        return sb.toString();
    }
}
