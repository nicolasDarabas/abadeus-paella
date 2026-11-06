package br.com.abadeus.paellaapi.repository;

import br.com.abadeus.paellaapi.domain.entity.Notificacao;
import br.com.abadeus.paellaapi.domain.enums.TipoNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByUsuarioIdOrderByEnviadaEmDesc(Long usuarioId);

    List<Notificacao> findAllByOrderByEnviadaEmDesc();

    boolean existsByPedidoIdAndTipo(Long pedidoId, TipoNotificacao tipo);
}
