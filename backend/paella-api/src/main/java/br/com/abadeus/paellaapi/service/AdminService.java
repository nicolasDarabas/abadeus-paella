package br.com.abadeus.paellaapi.service;

import br.com.abadeus.paellaapi.domain.entity.Evento;
import br.com.abadeus.paellaapi.domain.entity.Produto;
import br.com.abadeus.paellaapi.domain.entity.PerfilUsuario;
import br.com.abadeus.paellaapi.domain.entity.Usuario;
import br.com.abadeus.paellaapi.domain.enums.PerfilNome;
import br.com.abadeus.paellaapi.dto.AuthDtos.NovoUsuarioRequest;
import br.com.abadeus.paellaapi.exception.ConflitoException;
import br.com.abadeus.paellaapi.repository.PerfilUsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import br.com.abadeus.paellaapi.dto.AdminDtos.ProdutoVendidoResponse;
import br.com.abadeus.paellaapi.dto.AdminDtos.ResumoEventoResponse;
import br.com.abadeus.paellaapi.dto.AuthDtos.UsuarioResponse;
import br.com.abadeus.paellaapi.exception.NaoEncontradoException;
import br.com.abadeus.paellaapi.exception.RegraNegocioException;
import br.com.abadeus.paellaapi.repository.PedidoRepository;
import br.com.abadeus.paellaapi.repository.ProdutoRepository;
import br.com.abadeus.paellaapi.repository.UsuarioRepository;
import br.com.abadeus.paellaapi.security.SecurityUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final EventoService eventoService;
    private final EstoqueService estoqueService;
    private final PerfilUsuarioRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UsuarioRepository usuarioRepository, PedidoRepository pedidoRepository,
                        ProdutoRepository produtoRepository, EventoService eventoService,
                        EstoqueService estoqueService, PerfilUsuarioRepository perfilRepository,
                        PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.eventoService = eventoService;
        this.estoqueService = estoqueService;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Administrador cadastra usuarios operacionais (voluntario, outro administrador) ou compradores. */
    @Transactional
    public UsuarioResponse criarUsuario(NovoUsuarioRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email) || usuarioRepository.existsByLoginIgnoreCase(email)) {
            throw new ConflitoException("Ja existe um usuario com este e-mail.");
        }
        PerfilNome nomePerfil = req.perfil() == null ? PerfilNome.VOLUNTARIO : req.perfil();
        PerfilUsuario perfil = perfilRepository.findByNome(nomePerfil)
                .orElseThrow(() -> new NaoEncontradoException("Perfil", nomePerfil));
        Usuario u = new Usuario();
        u.setPerfil(perfil);
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setLogin(email);
        u.setTelefone(req.telefone());
        u.setSenhaHash(passwordEncoder.encode(req.senha()));
        u.setAtivo(true);
        return UsuarioResponse.de(usuarioRepository.save(u));
    }

    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll(Sort.by("nome")).stream().map(UsuarioResponse::de).toList();
    }

    @Transactional
    public UsuarioResponse alterarAtivo(Long id, boolean ativo) {
        if (!ativo && SecurityUtils.usuarioAtual().id().equals(id)) {
            throw new RegraNegocioException("Voce nao pode desativar o proprio usuario.");
        }
        Usuario u = usuarioRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("Usuario", id));
        u.setAtivo(ativo);
        return UsuarioResponse.de(u);
    }

    /** Painel do evento: pedidos por status, arrecadacao, retiradas e vendas por produto. */
    public ResumoEventoResponse resumo(Long eventoId) {
        Evento evento = eventoService.buscarEntidade(eventoId);

        Map<StatusPedido, Long> porStatus = new EnumMap<>(StatusPedido.class);
        long total = 0;
        for (StatusPedido s : StatusPedido.values()) {
            long qtd = pedidoRepository.countByEventoIdAndStatus(eventoId, s);
            porStatus.put(s, qtd);
            total += qtd;
        }

        BigDecimal arrecadado = pedidoRepository.somarValor(eventoId, EnumSet.of(StatusPedido.PAGO, StatusPedido.RETIRADO));
        BigDecimal pendente = pedidoRepository.somarValor(eventoId, EnumSet.of(StatusPedido.AGUARDANDO_PAGAMENTO));

        List<ProdutoVendidoResponse> produtos = produtoRepository.findByEventoIdOrderByNomeAsc(eventoId).stream()
                .map((Produto p) -> new ProdutoVendidoResponse(p.getId(), p.getNome(),
                        estoqueService.vendidos(p.getId()), p.getLimiteEstoque(), estoqueService.disponivel(p)))
                .toList();

        return new ResumoEventoResponse(evento.getId(), evento.getTitulo(), total, porStatus,
                arrecadado, pendente, porStatus.get(StatusPedido.RETIRADO), porStatus.get(StatusPedido.PAGO),
                produtos);
    }
}
