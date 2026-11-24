package br.ufs.dcomp.collina.service.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.ufs.dcomp.collina.dto.financeiro.RelatorioFechamentoResponseDTO;
import br.ufs.dcomp.collina.dto.financeiro.RelatorioInadimplenteResponseDTO;
import br.ufs.dcomp.collina.model.financeiro.MovimentacaoFinanceira;
import br.ufs.dcomp.collina.model.financeiro.OrigemMovimentacaoEnum;
import br.ufs.dcomp.collina.model.financeiro.StatusMensalidadeEnum;
import br.ufs.dcomp.collina.model.financeiro.TipoMovimentacaoEnum;
import br.ufs.dcomp.collina.repository.financeiro.MensalidadeRepository;
import br.ufs.dcomp.collina.repository.financeiro.MovimentacaoFinanceiraRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.stream.Collectors;
import java.util.List;
import java.time.format.DateTimeParseException;
import java.util.Comparator;

@Service
public class RelatorioFinanceiroService {
    
    @Autowired 
    private MovimentacaoFinanceiraRepository movimentacaoRepository;

    @Autowired 
    private MensalidadeRepository mensalidadeRepository;

    private record Chave(TipoMovimentacaoEnum tipo, OrigemMovimentacaoEnum origem) {}

    // RF-F03 - Emissão de relatório de fechamento financeiro
    @Transactional(readOnly = true)
    public RelatorioFechamentoResponseDTO gerarFechamento(String competencia) {

        // Captura o mês da competência
        YearMonth mes;
        try {
            mes = YearMonth.parse(competencia);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Competência inválida. Use o formato AAAA-MM, por exemplo 2026-11");
        }

        // Canceladas não fazem parte do relatório
        List<MovimentacaoFinanceira> ativas = movimentacaoRepository
                .buscarPorCompetencia(mes.toString())
                .stream()
                .filter(MovimentacaoFinanceira::estaAtiva)
                .toList();

        // Conta o total de entradas e de saídas
        BigDecimal totalEntradas = ativas.stream()
                .filter(m -> m.getTipo() == TipoMovimentacaoEnum.ENTRADA)
                .map(MovimentacaoFinanceira::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSaidas = ativas.stream()
                .filter(m -> m.getTipo() == TipoMovimentacaoEnum.SAIDA)
                .map(MovimentacaoFinanceira::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Geração do relatório
        List<RelatorioFechamentoResponseDTO.ItemCategoriaDTO> porCategoria = ativas.stream()
                .collect(Collectors.groupingBy(
                        m -> new Chave(m.getTipo(), m.getOrigem()),
                        Collectors.reducing(BigDecimal.ZERO,
                                MovimentacaoFinanceira::getValor, BigDecimal::add)))
                .entrySet().stream()
                .map(e -> new RelatorioFechamentoResponseDTO.ItemCategoriaDTO(
                        e.getKey().tipo(), e.getKey().origem(), e.getValue()))
                .sorted(Comparator
                        .comparing(RelatorioFechamentoResponseDTO.ItemCategoriaDTO::tipo)
                        .thenComparing(RelatorioFechamentoResponseDTO.ItemCategoriaDTO::origem))
                .toList();

        return new RelatorioFechamentoResponseDTO(
                mes.toString(), totalEntradas, totalSaidas,
                totalEntradas.subtract(totalSaidas), porCategoria);
    }

    // RF-F15 - Consulta de associados inadimplentes (mensalidades com status ATRASADA)
    @Transactional(readOnly = true)
    public List<RelatorioInadimplenteResponseDTO> listarInadimplentes() {
        return mensalidadeRepository.findByStatusMensalidadeOrderByDataVencimentoAsc(StatusMensalidadeEnum.ATRASADA)
                .stream()
                .map(RelatorioInadimplenteResponseDTO::new)
                .toList();
    }
    
}
