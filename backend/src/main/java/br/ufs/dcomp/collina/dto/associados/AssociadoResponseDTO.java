package br.ufs.dcomp.collina.dto.associados;

import br.ufs.dcomp.collina.model.associados.Associado;
import br.ufs.dcomp.collina.model.associados.CategoriaAssociadoEnum;
import br.ufs.dcomp.collina.model.associados.SituacaoCadastralEnum;

import java.time.LocalDate;

public record AssociadoResponseDTO(
        Integer id,
        PessoaResponseDTO pessoa,
        CategoriaAssociadoEnum categoria,
        PlanoResponseDTO plano,
        SituacaoCadastralEnum situacaoCadastral,
        LocalDate dataAdmissao,
        String usuarioId) {

    public AssociadoResponseDTO(Associado associado) {
        this(
                associado.getId(),
                new PessoaResponseDTO(associado.getPessoa()),
                associado.getCategoria(),
                new PlanoResponseDTO(associado.getPlano()),
                associado.getSituacaoCadastral(),
                associado.getDataAdmissao(),
                associado.getUsuarioId()
        );
    }
}
