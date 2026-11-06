package br.com.abadeus.paellaapi.repository;

import br.com.abadeus.paellaapi.domain.entity.Evento;
import br.com.abadeus.paellaapi.domain.enums.StatusEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findByStatusInOrderByDataEventoAsc(Collection<StatusEvento> status);

    List<Evento> findAllByOrderByDataEventoDesc();
}
