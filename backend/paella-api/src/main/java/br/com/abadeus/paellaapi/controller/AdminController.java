package br.com.abadeus.paellaapi.controller;

import br.com.abadeus.paellaapi.domain.enums.StatusPedido;
import br.com.abadeus.paellaapi.dto.AdminDtos.ResumoEventoResponse;
import br.com.abadeus.paellaapi.dto.AuthDtos.AtivoRequest;
import br.com.abadeus.paellaapi.dto.AuthDtos.UsuarioResponse;
import br.com.abadeus.paellaapi.dto.PedidoDtos.PedidoResponse;
import br.com.abadeus.paellaapi.dto.RetiradaDtos.ConsultaRetiradaResponse;
import br.com.abadeus.paellaapi.dto.RetiradaDtos.RetiradaResponse;
import br.com.abadeus.paellaapi.dto.RetiradaDtos.ValidarRetiradaRequest;
import br.com.abadeus.paellaapi.dto.AuthDtos.NovoUsuarioRequest;
import br.com.abadeus.paellaapi.dto.PedidoDtos.CancelarRequest;
import br.com.abadeus.paellaapi.dto.PedidoDtos.NotificacaoResponse;
import br.com.abadeus.paellaapi.service.AdminService;
import br.com.abadeus.paellaapi.service.NotificacaoService;
import br.com.abadeus.paellaapi.service.RelatorioService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.nio.charset.StandardCharsets;
import br.com.abadeus.paellaapi.service.PedidoService;
import br.com.abadeus.paellaapi.service.RetiradaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Visao do Administrador. Todas as rotas /api/admin/** exigem perfil ADMINISTRADOR. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final PedidoService pedidoService;
    private final RetiradaService retiradaService;
    private final RelatorioService relatorioService;
    private final NotificacaoService notificacaoService;

    public AdminController(AdminService adminService, PedidoService pedidoService, RetiradaService retiradaService,
                           RelatorioService relatorioService, NotificacaoService notificacaoService) {
        this.adminService = adminService;
        this.pedidoService = pedidoService;
        this.retiradaService = retiradaService;
        this.relatorioService = relatorioService;
        this.notificacaoService = notificacaoService;
    }

    // ---------- Painel ----------

    @GetMapping("/eventos/{id}/resumo")
    public ResumoEventoResponse resumo(@PathVariable Long id) {
        return adminService.resumo(id);
    }

    // ---------- Pedidos ----------

    @GetMapping("/pedidos")
    public List<PedidoResponse> pedidos(@RequestParam(required = false) Long eventoId,
                                       @RequestParam(required = false) StatusPedido status) {
        return pedidoService.listarAdmin(eventoId, status);
    }

    /** Reembolso de pedido pago (ainda nao retirado). */
    @PostMapping("/pedidos/{id}/reembolsar")
    public PedidoResponse reembolsar(@PathVariable Long id, @RequestBody(required = false) CancelarRequest req) {
        return pedidoService.reembolsar(id, req == null ? null : req.motivo());
    }

    // ---------- Relatorios (CSV) ----------

    @GetMapping("/relatorios/pedidos.csv")
    public ResponseEntity<byte[]> relatorioPedidos(@RequestParam Long eventoId) {
        return csv("pedidos-evento-" + eventoId + ".csv", relatorioService.pedidosCsv(eventoId));
    }

    @GetMapping("/relatorios/arrecadacao.csv")
    public ResponseEntity<byte[]> relatorioArrecadacao(@RequestParam Long eventoId) {
        return csv("arrecadacao-evento-" + eventoId + ".csv", relatorioService.arrecadacaoCsv(eventoId));
    }

    private static ResponseEntity<byte[]> csv(String nome, String conteudo) {
        // BOM UTF-8: faz o Excel abrir os acentos corretamente
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] corpo = conteudo.getBytes(StandardCharsets.UTF_8);
        byte[] tudo = new byte[bom.length + corpo.length];
        System.arraycopy(bom, 0, tudo, 0, bom.length);
        System.arraycopy(corpo, 0, tudo, bom.length, corpo.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(tudo);
    }

    // ---------- Comunicacao ----------

    /** Todos os avisos enviados (e-mail simulado). */
    @GetMapping("/notificacoes")
    public List<NotificacaoResponse> notificacoes() {
        return notificacaoService.todas();
    }

    // ---------- Usuarios ----------

    /** Cadastro de usuario pelo administrador (padrao: perfil VOLUNTARIO). */
    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criarUsuario(@Valid @RequestBody NovoUsuarioRequest req) {
        return adminService.criarUsuario(req);
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> usuarios() {
        return adminService.listarUsuarios();
    }

    @PatchMapping("/usuarios/{id}/ativo")
    public UsuarioResponse alterarAtivo(@PathVariable Long id, @RequestBody AtivoRequest req) {
        return adminService.alterarAtivo(id, req.ativo());
    }

    // ---------- Retirada (dia do evento) ----------

    /** Depois de ler o QR: mostra o pedido e se pode entregar. */
    @GetMapping("/retiradas/consultar")
    public ConsultaRetiradaResponse consultar(@RequestParam String codigo) {
        return retiradaService.consultar(codigo);
    }

    /** Confirma a entrega. Uma segunda tentativa retorna 409 (ja retirado). */
    @PostMapping("/retiradas/validar")
    public RetiradaResponse validar(@Valid @RequestBody ValidarRetiradaRequest req) {
        return retiradaService.validar(req.codigo(), req.metodo());
    }

    @GetMapping("/eventos/{id}/retiradas")
    public List<RetiradaResponse> retiradas(@PathVariable Long id) {
        return retiradaService.listarPorEvento(id);
    }
}
