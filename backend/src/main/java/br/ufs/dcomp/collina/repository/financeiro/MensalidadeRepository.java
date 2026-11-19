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
    
    List<Mensalidade> findByAssociadoTitularAndCompetencia(Long associadoTitular, String competencia);

    Optional<Mensalidade> findByAssociadoTitularAndStatusMensalidade(Long associadoTitular, StatusMensalidadeEnum status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mensalidade m where m.id = :id")
    Optional<Mensalidade> buscarPorIdParaAtualizar(@Param("id") Long id);

    default Optional<Mensalidade> buscarEmAbertoDoTitular(Long associadoTitular){
        return findByAssociadoTitularAndStatusMensalidade(associadoTitular, StatusMensalidadeEnum.EM_ABERTO);
    }
    
    default List<Mensalidade> buscarMensalidadesPendentesVencidas(LocalDate data){
        return findByStatusMensalidadeAndDataVencimentoLessThan(StatusMensalidadeEnum.PENDENTE, data);
    }
}
