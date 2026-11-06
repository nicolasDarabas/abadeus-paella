package br.com.abadeus.paellaapi.repository;

import br.com.abadeus.paellaapi.domain.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findByEventoIdOrderByNomeAsc(Long eventoId);

    List<Produto> findByEventoIdAndAtivoTrueOrderByNomeAsc(Long eventoId);
}
