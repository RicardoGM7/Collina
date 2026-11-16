package br.ufs.dcomp.collina.repository.financeiro;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.model.financeiro.StatusMensalidadeEnum;



public interface MensalidadeRepository extends JpaRepository<Mensalidade, Long>{
    
    List<Mensalidade> findByStatusMensalidadeAndDataVencimento(StatusMensalidadeEnum statusMensalidade, LocalDate data);
    
    List<Mensalidade> findByAssociadoTitularAndCompetencia(Long associadoTitular, String competencia);

    Optional<Mensalidade> findByAssociadoTitularAndStatusMensalidade(Long associadoTitular, StatusMensalidadeEnum status);

    default Optional<Mensalidade> buscarEmAbertoDoTitular(Long associadoTitular){
        return findByAssociadoTitularAndStatusMensalidade(associadoTitular, StatusMensalidadeEnum.EM_ABERTO);
    }
    
    default List<Mensalidade> buscarMensalidadesPendentesVencidas(LocalDate data){
        return findByStatusMensalidadeAndDataVencimento(StatusMensalidadeEnum.PENDENTE, data);
    }
}
