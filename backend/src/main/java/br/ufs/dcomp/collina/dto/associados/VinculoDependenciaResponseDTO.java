package br.ufs.dcomp.collina.dto.associados;

import br.ufs.dcomp.collina.model.associados.MotivoInativacaoVinculoEnum;
import br.ufs.dcomp.collina.model.associados.StatusVinculoEnum;
import br.ufs.dcomp.collina.model.associados.VinculoDependencia;

import java.time.LocalDate;

public record VinculoDependenciaResponseDTO(
        Integer id,
        Integer titularId,
        PessoaResponseDTO dependente,
        String parentesco,
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusVinculoEnum status,
        MotivoInativacaoVinculoEnum motivoInativacao) {

    public VinculoDependenciaResponseDTO(VinculoDependencia vinculo) {
        this(
                vinculo.getId(),
                vinculo.getTitular().getId(),
                new PessoaResponseDTO(vinculo.getDependente()),
                vinculo.getParentesco(),
                vinculo.getDataInicio(),
                vinculo.getDataFim(),
                vinculo.getStatus(),
                vinculo.getMotivoInativacao()
        );
    }
}
