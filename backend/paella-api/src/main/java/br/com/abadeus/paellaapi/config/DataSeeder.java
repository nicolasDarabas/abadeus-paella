package br.com.abadeus.paellaapi.config;

import br.com.abadeus.paellaapi.domain.entity.Evento;
import br.com.abadeus.paellaapi.domain.entity.Lote;
import br.com.abadeus.paellaapi.domain.entity.LoteProduto;
import br.com.abadeus.paellaapi.domain.entity.PerfilUsuario;
import br.com.abadeus.paellaapi.domain.entity.Produto;
import br.com.abadeus.paellaapi.domain.entity.Usuario;
import br.com.abadeus.paellaapi.domain.enums.PerfilNome;
import br.com.abadeus.paellaapi.domain.enums.StatusEvento;
import br.com.abadeus.paellaapi.repository.EventoRepository;
import br.com.abadeus.paellaapi.repository.LoteProdutoRepository;
import br.com.abadeus.paellaapi.repository.LoteRepository;
import br.com.abadeus.paellaapi.repository.PerfilUsuarioRepository;
import br.com.abadeus.paellaapi.repository.ProdutoRepository;
import br.com.abadeus.paellaapi.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Dados iniciais criados na primeira execucao:
 * - os 2 perfis (ADMINISTRADOR e COMPRADOR)
 * - o usuario administrador (login ADMINISTRADOR / senha ADM123, gravada com BCrypt)
 * - opcionalmente, um evento de exemplo com produtos e lote (app.seed.dados-exemplo=true)
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String CONTATO_PADRAO = "WhatsApp (11) 99999-0000 \u00b7 contato@abadeus.org.br";
    private static final String REGRAS_PADRAO = "Apresente o QR Code ou o n\u00famero do pedido. "
            + "Cada pedido pode ser retirado uma \u00fanica vez, dentro do hor\u00e1rio de retirada. "
            + "Traga sua sacola; a paella \u00e9 entregue em embalagem descart\u00e1vel.";

    private final PerfilUsuarioRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final EventoRepository eventoRepository;
    private final ProdutoRepository produtoRepository;
    private final LoteRepository loteRepository;
    private final LoteProdutoRepository loteProdutoRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.login}")
    private String adminLogin;
    @Value("${app.admin.senha}")
    private String adminSenha;
    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.voluntario.login:VOLUNTARIO}")
    private String voluntarioLogin;
    @Value("${app.voluntario.senha:VOL123}")
    private String voluntarioSenha;
    @Value("${app.voluntario.email:voluntario@abadeus.org.br}")
    private String voluntarioEmail;
    @Value("${app.seed.dados-exemplo:true}")
    private boolean dadosExemplo;

    public DataSeeder(PerfilUsuarioRepository perfilRepository, UsuarioRepository usuarioRepository,
                      EventoRepository eventoRepository, ProdutoRepository produtoRepository,
                      LoteRepository loteRepository, LoteProdutoRepository loteProdutoRepository,
                      PasswordEncoder passwordEncoder) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
        this.produtoRepository = produtoRepository;
        this.loteRepository = loteRepository;
        this.loteProdutoRepository = loteProdutoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        PerfilUsuario admin = perfil(PerfilNome.ADMINISTRADOR, "Organizacao da ABADEUS: gerencia evento, vendas e retiradas");
        PerfilUsuario comprador = perfil(PerfilNome.COMPRADOR, "Pessoa que compra a paella pelo app ou site");
        PerfilUsuario voluntario = perfil(PerfilNome.VOLUNTARIO, "Equipe de entrega: valida as retiradas no dia do evento");

        if (usuarioRepository.findByLoginOuEmail(voluntarioLogin).isEmpty()) {
            usuario(voluntario, "Volunt\u00e1rio da Retirada", voluntarioLogin, voluntarioEmail, voluntarioSenha);
            log.info(">>> Usuario voluntario criado: login '{}'", voluntarioLogin);
        }

        if (usuarioRepository.findByLoginOuEmail(adminLogin).isEmpty()) {
            usuario(admin, "Administrador ABADEUS", adminLogin, adminEmail, adminSenha);
            log.info(">>> Usuario administrador criado: login '{}'", adminLogin);
        }

        corrigirTextosAntigos();

        if (dadosExemplo && eventoRepository.count() == 0) {
            criarEventoExemplo();
            if (usuarioRepository.findByLoginOuEmail("comprador@teste.com").isEmpty()) {
                usuario(comprador, "Comprador de Teste", "comprador@teste.com", "comprador@teste.com", "123456");
            }
            log.info(">>> Evento de exemplo e comprador de teste (comprador@teste.com / 123456) criados");
        }
    }

    /**
     * Bancos criados por versoes antigas tinham os nomes sem acento.
     * Aqui corrigimos esses textos automaticamente ao iniciar a API.
     */
    private void corrigirTextosAntigos() {
        java.util.Map<String, String> nomes = new java.util.HashMap<>();
        nomes.put("Paella Valenciana - porcao individual", "Paella Valenciana \u2013 por\u00e7\u00e3o individual");
        nomes.put("Paella Valenciana - familia (4 porcoes)", "Paella Valenciana \u2013 fam\u00edlia (4 por\u00e7\u00f5es)");
        nomes.put("Paella Vegetariana - porcao individual", "Paella Vegetariana \u2013 por\u00e7\u00e3o individual");
        nomes.put("Sangria sem alcool 500 ml", "Sangria sem \u00e1lcool 500 ml");
        java.util.Map<String, String> descricoes = new java.util.HashMap<>();
        descricoes.put("Arroz com acafrao, frango, frutos do mar e legumes. Serve 1 pessoa.",
                "Arroz com a\u00e7afr\u00e3o, frango, frutos do mar e legumes. Serve 1 pessoa.");
        descricoes.put("Embalagem familia, serve 4 pessoas.", "Embalagem fam\u00edlia, serve 4 pessoas.");
        descricoes.put("Arroz com acafrao e legumes da estacao. Serve 1 pessoa.",
                "Arroz com a\u00e7afr\u00e3o e legumes da esta\u00e7\u00e3o. Serve 1 pessoa.");
        descricoes.put("Bebida de frutas, sem alcool.", "Bebida de frutas, sem \u00e1lcool.");

        int corrigidos = 0;
        for (Produto p : produtoRepository.findAll()) {
            boolean mudou = false;
            if (nomes.containsKey(p.getNome())) {
                p.setNome(nomes.get(p.getNome()));
                mudou = true;
            }
            if (p.getDescricao() != null && descricoes.containsKey(p.getDescricao())) {
                p.setDescricao(descricoes.get(p.getDescricao()));
                mudou = true;
            }
            if (mudou) {
                corrigidos++;
            }
        }
        for (Lote l : loteRepository.findAll()) {
            if ("1o Lote promocional".equals(l.getNome())) {
                l.setNome("1\u00ba Lote promocional");
                corrigidos++;
            }
        }
        for (Evento ev : eventoRepository.findAll()) {
            if (ev.getContato() == null) {
                ev.setContato(CONTATO_PADRAO);
                ev.setRegras(REGRAS_PADRAO);
                corrigidos++;
            }
            if (ev.getDescricao() != null && ev.getDescricao().contains("voluntarios")) {
                ev.setDescricao("Paella tradicional preparada por volunt\u00e1rios. Toda a renda \u00e9 revertida para os projetos da ABADEUS.");
                corrigidos++;
            }
        }
        if (corrigidos > 0) {
            log.info(">>> {} texto(s) antigo(s) corrigido(s) com acentuacao", corrigidos);
        }
    }

    private PerfilUsuario perfil(PerfilNome nome, String descricao) {
        return perfilRepository.findByNome(nome)
                .orElseGet(() -> perfilRepository.save(new PerfilUsuario(nome, descricao)));
    }

    private void usuario(PerfilUsuario perfil, String nome, String login, String email, String senha) {
        Usuario u = new Usuario();
        u.setPerfil(perfil);
        u.setNome(nome);
        u.setLogin(login);
        u.setEmail(email);
        u.setSenhaHash(passwordEncoder.encode(senha));
        u.setAtivo(true);
        usuarioRepository.save(u);
    }

    private void criarEventoExemplo() {
        LocalDate dia = LocalDate.now().plusDays(30);

        Evento e = new Evento();
        e.setTitulo("Paella Beneficente ABADEUS " + dia.getYear());
        e.setDescricao("Paella tradicional preparada por volunt\u00e1rios. Toda a renda \u00e9 revertida para os projetos da ABADEUS.");
        e.setLocal("Sede da ABADEUS");
        e.setDataEvento(dia.atTime(LocalTime.of(12, 0)));
        e.setRetiradaInicio(dia.atTime(LocalTime.of(11, 0)));
        e.setRetiradaFim(dia.atTime(LocalTime.of(15, 0)));
        e.setContato(CONTATO_PADRAO);
        e.setRegras(REGRAS_PADRAO);
        e.setStatus(StatusEvento.VENDAS_ABERTAS);
        eventoRepository.save(e);

        Produto individual = produto(e, "Paella Valenciana \u2013 por\u00e7\u00e3o individual",
                "Arroz com a\u00e7afr\u00e3o, frango, frutos do mar e legumes. Serve 1 pessoa.", "45.00", 300);
        produto(e, "Paella Valenciana \u2013 fam\u00edlia (4 por\u00e7\u00f5es)",
                "Embalagem fam\u00edlia, serve 4 pessoas.", "160.00", 80);
        produto(e, "Paella Vegetariana \u2013 por\u00e7\u00e3o individual",
                "Arroz com a\u00e7afr\u00e3o e legumes da esta\u00e7\u00e3o. Serve 1 pessoa.", "40.00", 60);
        produto(e, "Sangria sem \u00e1lcool 500 ml", "Bebida de frutas, sem \u00e1lcool.", "12.00", null);

        Lote lote = new Lote();
        lote.setEvento(e);
        lote.setNome("1\u00ba Lote promocional");
        lote.setVendaInicio(LocalDateTime.now().minusDays(1));
        lote.setVendaFim(LocalDateTime.now().plusDays(15));
        lote.setAtivo(true);
        loteRepository.save(lote);

        LoteProduto lp = new LoteProduto();
        lp.setLote(lote);
        lp.setProduto(individual);
        lp.setPrecoLote(new BigDecimal("40.00"));
        lp.setQuantidade(100);
        loteProdutoRepository.save(lp);
    }

    private Produto produto(Evento e, String nome, String descricao, String preco, Integer limite) {
        Produto p = new Produto();
        p.setEvento(e);
        p.setNome(nome);
        p.setDescricao(descricao);
        p.setPreco(new BigDecimal(preco));
        p.setLimiteEstoque(limite);
        p.setAtivo(true);
        return produtoRepository.save(p);
    }
}
