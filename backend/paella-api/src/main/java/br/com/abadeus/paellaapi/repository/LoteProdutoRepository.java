package br.com.abadeus.paellaapi.repository;

import br.com.abadeus.paellaapi.domain.entity.LoteProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoteProdutoRepository extends JpaRepository<LoteProduto, Long> {

    List<LoteProduto> findByLoteId(Long loteId);

    Optional<LoteProduto> findByLoteIdAndProdutoId(Long loteId, Long produtoId);

    /** Lotes vigentes (ativos e dentro do periodo de venda) que contem o produto. */
    @Query("""
            select lp from LoteProduto lp
            join fetch lp.lote l
            where lp.produto.id = :produtoId
              and l.ativo = true
              and l.vendaInicio <= :agora
              and l.vendaFim >= :agora
            order by l.vendaInicio asc
            """)
    List<LoteProduto> findVigentes(@Param("produtoId") Long produtoId, @Param("agora") LocalDateTime agora);
}
