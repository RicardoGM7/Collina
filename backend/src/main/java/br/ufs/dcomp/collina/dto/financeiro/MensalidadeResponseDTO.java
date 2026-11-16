package br.ufs.dcomp.collina.dto.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.model.financeiro.StatusMensalidadeEnum;

public record MensalidadeResponseDTO(

    Long id,
    String competencia,
    StatusMensalidadeEnum status,
    BigDecimal valor,
    LocalDate vencimento,
    Long associadoId

) {
    public MensalidadeResponseDTO(Mensalidade mensalidade){

        this(
            mensalidade.getId(), 
            mensalidade.getCompetencia(), 
            mensalidade.getStatusMensalidade(), 
            mensalidade.estaEmAberto() ? mensalidade.calcularValorTotal() : mensalidade.getValorFinal(), 
            mensalidade.getDataVencimento(), 
            mensalidade.getAssociadoTitular()
        );
    }
}
