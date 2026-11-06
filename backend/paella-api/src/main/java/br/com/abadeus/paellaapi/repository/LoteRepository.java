package br.com.abadeus.paellaapi.repository;

import br.com.abadeus.paellaapi.domain.entity.Lote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoteRepository extends JpaRepository<Lote, Long> {
    List<Lote> findByEventoIdOrderByVendaInicioAsc(Long eventoId);
}
