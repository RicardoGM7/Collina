package br.ufs.dcomp.collina.repository.financeiro;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.model.financeiro.StatusMensalidadeEnum;
import jakarta.persistence.LockModeType;



public interface MensalidadeRepository extends JpaRepository<Mensalidade, Long>{
    
    List<Mensalidade> findByStatusMensalidadeAndDataVencimentoLessThan(StatusMensalidadeEnum statusMensalidade, LocalDate data);
    
    @Query("""
        select m from Mensalidade m
        left join fetch m.associadoTitular a
        left join fetch a.pessoa p
        where m.statusMensalidade = :status
        order by m.dataVencimento asc, m.id asc
        """)
    List<Mensalidade> findByStatusMensalidadeOrderByDataVencimentoAsc(@Param("status") StatusMensalidadeEnum status);

    List<Mensalidade> findByStatusMensalidade(StatusMensalidadeEnum status);

    List<Mensalidade> findByAssociadoTitular_IdAndCompetencia(Integer associadoTitularId, String competencia);

    default List<Mensalidade> findByAssociadoTitularIdAndCompetencia(Integer associadoTitularId, String competencia) {
        return findByAssociadoTitular_IdAndCompetencia(associadoTitularId, competencia);
    }

    Optional<Mensalidade> findByAssociadoTitular_IdAndStatusMensalidade(
            Integer associadoTitularId, StatusMensalidadeEnum status);

    default Optional<Mensalidade> findByAssociadoTitularIdAndStatusMensalidade(
            Integer associadoTitularId, StatusMensalidadeEnum status) {
        return findByAssociadoTitular_IdAndStatusMensalidade(associadoTitularId, status);
    }

    List<Mensalidade> findByAssociadoTitular_Id(Integer associadoTitularId);

    default List<Mensalidade> findByAssociadoTitularId(Integer associadoTitularId) {
        return findByAssociadoTitular_Id(associadoTitularId);
    }

    List<Mensalidade> findByAssociadoTitular_IdOrderByCompetenciaDesc(Integer associadoTitularId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mensalidade m where m.id = :id")
    Optional<Mensalidade> buscarPorIdParaAtualizar(@Param("id") Long id);

    default Optional<Mensalidade> buscarEmAbertoDoTitular(Integer associadoTitularId){
        return findByAssociadoTitular_IdAndStatusMensalidade(
                associadoTitularId, StatusMensalidadeEnum.EM_ABERTO);
    }
    
    default List<Mensalidade> buscarMensalidadesPendentesVencidas(LocalDate data){
        return findByStatusMensalidadeAndDataVencimentoLessThan(StatusMensalidadeEnum.PENDENTE, data);
    }
}
