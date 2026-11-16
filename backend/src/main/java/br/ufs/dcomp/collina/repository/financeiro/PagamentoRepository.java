package br.ufs.dcomp.collina.repository.financeiro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.ufs.dcomp.collina.model.financeiro.Pagamento;
import br.ufs.dcomp.collina.model.financeiro.Mensalidade;

import java.util.List;



public interface PagamentoRepository extends JpaRepository<Pagamento, Long>{
    List<Pagamento> findByIdExternoGateway(String idExternoGateway);

    List<Pagamento> findByMensalidade(Mensalidade mensalidade);

    @Query("""
        select p from Pagamento p 
        where p.mensalidade.associadoTitular = :associadoId
        """)
    List<Pagamento> buscarPorTitular(@Param("associadoId") Long associadoId);
}
