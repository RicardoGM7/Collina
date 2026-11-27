package br.ufs.dcomp.collina.repository.associados;

import br.ufs.dcomp.collina.model.associados.MotivoInativacaoVinculoEnum;
import br.ufs.dcomp.collina.model.associados.StatusVinculoEnum;
import br.ufs.dcomp.collina.model.associados.VinculoDependencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import br.ufs.dcomp.collina.model.auth.Pessoa;

public interface VinculoDependenciaRepository extends JpaRepository<VinculoDependencia, Integer> {
    long countByTitularIdAndStatus(Integer titularId, StatusVinculoEnum status);
    List<VinculoDependencia> findByTitularId(Integer titularId);
    List<VinculoDependencia> findByTitularIdAndStatusAndMotivoInativacao(
            Integer titularId, StatusVinculoEnum status, MotivoInativacaoVinculoEnum motivo);
    Optional<VinculoDependencia> findByDependenteIdAndStatus(Integer dependenteId, StatusVinculoEnum status);
    // NOVO: usado pela rotina de inativação por idade (RF-AS04)
    List<VinculoDependencia> findByStatus(StatusVinculoEnum status);

    // Usado na inclusão de gastos avulsos dependente -> associado titular -> mensalidade
    Optional<VinculoDependencia> findByDependente(Pessoa dependente);
}
