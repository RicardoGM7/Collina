package br.ufs.dcomp.collina.repository.financeiro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.ufs.dcomp.collina.model.financeiro.Pagamento;
import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.model.financeiro.StatusTransacaoEnum;

import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long>{
    List<Pagamento> findByIdExternoGateway(String idExternoGateway);

    List<Pagamento> findByMensalidade(Mensalidade mensalidade);

    // Cobrança em andamento mais recente de uma mensalidade (para não criar duas ao mesmo tempo).
    Optional<Pagamento> findFirstByMensalidadeAndStatusOrderByIdDesc(Mensalidade mensalidade, StatusTransacaoEnum status);

    // O webhook só conhece o id externo. Primeiro se descobre a mensalidade (um id, não uma entidade),
    // para travá-la antes de ler o pagamento; assim o estado lido é o atual. Ver CobrancaTransacaoService.
    @Query("select p.mensalidade.id from Pagamento p where p.idExternoGateway = :idExterno")
    Optional<Long> buscarMensalidadeIdPorIdExterno(@Param("idExterno") String idExterno);

    @Query("select p from Pagamento p where p.idExternoGateway = :idExterno")
    Optional<Pagamento> buscarPorIdExterno(@Param("idExterno") String idExterno);

    @Query("""
        select p from Pagamento p 
        where p.mensalidade.associadoTitular = :associadoId
        """)
    List<Pagamento> buscarPorTitular(@Param("associadoId") Long associadoId);
}
