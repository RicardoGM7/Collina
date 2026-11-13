package br.ufs.dcomp.collina.repository.financeiro;

import br.ufs.dcomp.collina.model.financeiro.MovimentacaoFinanceira;
import br.ufs.dcomp.collina.model.financeiro.OrigemMovimentacaoEnum;
import br.ufs.dcomp.collina.model.financeiro.SituacaoMovimentacaoEnum;
import br.ufs.dcomp.collina.model.financeiro.TipoMovimentacaoEnum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.time.LocalDateTime;
import java.time.YearMonth;

public interface MovimentacaoFinanceiraRepository extends JpaRepository<MovimentacaoFinanceira, Long> {

    // Buscar por competência
    List<MovimentacaoFinanceira> findByDataHoraGreaterThanEqualAndDataHoraLessThan(LocalDateTime inicio, LocalDateTime fim);

    default List<MovimentacaoFinanceira> buscarPorCompetencia(String competencia) {

        YearMonth mes = YearMonth.parse(competencia);
        LocalDateTime inicio = mes.atDay(1).atStartOfDay();
        LocalDateTime fim = mes.plusMonths(1).atDay(1).atStartOfDay();

        return findByDataHoraGreaterThanEqualAndDataHoraLessThan(inicio, fim);
    }

    // Buscar por filtro
    @Query("""
       SELECT m FROM MovimentacaoFinanceira m
       WHERE (:tipo IS NULL OR m.tipo = :tipo)
         AND (:origem IS NULL OR m.origem = :origem)
         AND (:situacao IS NULL OR m.situacao = :situacao)
         AND m.dataHora >= :inicio
         AND m.dataHora < :fim
       ORDER BY m.dataHora DESC
       """)
    List<MovimentacaoFinanceira> buscarPorFiltro(TipoMovimentacaoEnum tipo,
           OrigemMovimentacaoEnum origem, SituacaoMovimentacaoEnum situacao,
           LocalDateTime inicio, LocalDateTime fim);
}