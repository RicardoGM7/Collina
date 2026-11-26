package br.ufs.dcomp.collina.service.funcionarios;

import br.ufs.dcomp.collina.dto.funcionarios.*;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.funcionarios.*;
import br.ufs.dcomp.collina.repository.funcionarios.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RHService {

    @Autowired
    private PresencaRepository presencaRepository;

    @Autowired
    private AlocacaoRepository alocacaoRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private FeriasRepository feriasRepository;

    @Autowired
    private AfastamentoRepository afastamentoRepository;

    // ==========================================
    // 1. Registro de Presença e Ponto (RF-FU08)
    // ==========================================

    @Transactional
    public PresencaResponseDTO registrarPresenca(PresencaRequestDTO dto) {
        if (dto == null || dto.alocacaoId() == null) {
            throw new IllegalArgumentException("O ID da alocação é obrigatório.");
        }

        Alocacao alocacao = alocacaoRepository.findById(dto.alocacaoId())
                .orElseThrow(() -> new ResourceNotFoundException("Alocação não encontrada com ID: " + dto.alocacaoId()));

        if (alocacao.getStatus() == StatusAlocacao.CANCELADA) {
            throw new IllegalStateException("Não é possível registrar presença em uma alocação CANCELADA.");
        }

        Presenca presenca = presencaRepository.findByAlocacaoId(dto.alocacaoId())
                .orElse(new Presenca());

        presenca.setAlocacao(alocacao);
        presenca.setEntrada(dto.entrada());
        presenca.setSaida(dto.saida());

        if (dto.status() != null) {
            presenca.setStatus(dto.status());
        } else if (dto.entrada() != null && dto.saida() != null) {
            presenca.setStatus(StatusPresenca.PRESENTE);
        } else if (dto.entrada() != null) {
            presenca.setStatus(StatusPresenca.PARCIAL);
        } else {
            presenca.setStatus(StatusPresenca.AUSENTE);
        }

        Presenca salva = presencaRepository.save(presenca);
        return new PresencaResponseDTO(salva);
    }

    @Transactional
    public PresencaResponseDTO registrarAusencia(AusenciaRequestDTO dto) {
        if (dto == null || dto.alocacaoId() == null) {
            throw new IllegalArgumentException("O ID da alocação é obrigatório.");
        }

        Alocacao alocacao = alocacaoRepository.findById(dto.alocacaoId())
                .orElseThrow(() -> new ResourceNotFoundException("Alocação não encontrada com ID: " + dto.alocacaoId()));

        Presenca presenca = presencaRepository.findByAlocacaoId(dto.alocacaoId())
                .orElse(new Presenca());

        presenca.setAlocacao(alocacao);
        presenca.setEntrada(null);
        presenca.setSaida(null);
        presenca.setStatus(dto.justificada() ? StatusPresenca.JUSTIFICADA : StatusPresenca.AUSENTE);

        Presenca salva = presencaRepository.save(presenca);
        return new PresencaResponseDTO(salva);
    }

    @Transactional(readOnly = true)
    public PresencaResponseDTO buscarPresencaPorAlocacao(Integer alocacaoId) {
        Presenca presenca = presencaRepository.findByAlocacaoId(alocacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum registro de presença encontrado para a alocação ID: " + alocacaoId));
        return new PresencaResponseDTO(presenca);
    }

    // ==========================================
    // 2. Relatório de Presença e Ausência (RF-FU09)
    // ==========================================

    @Transactional(readOnly = true)
    public RelatorioPresencaResponseDTO gerarRelatorio(LocalDate dataInicio, LocalDate dataFim) {
        LocalDate inicio = dataInicio != null ? dataInicio : LocalDate.now().minusMonths(1);
        LocalDate fim = dataFim != null ? dataFim : LocalDate.now();

        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }

        List<Presenca> presencas = presencaRepository.findByPeriodo(inicio, fim);

        long totalPresencas = presencas.stream()
                .filter(p -> p.getStatus() == StatusPresenca.PRESENTE || p.getStatus() == StatusPresenca.PARCIAL)
                .count();

        long totalAusencias = presencas.stream()
                .filter(p -> p.getStatus() == StatusPresenca.AUSENTE || p.getStatus() == StatusPresenca.JUSTIFICADA)
                .count();

        long totalRegistros = presencas.size();
        double porcentagemPresenca = totalRegistros > 0 ? ((double) totalPresencas / totalRegistros) * 100.0 : 0.0;

        // Ranking mais presentes
        Map<Integer, Long> contagemPresencasPorFunc = presencas.stream()
                .filter(p -> p.getStatus() == StatusPresenca.PRESENTE || p.getStatus() == StatusPresenca.PARCIAL)
                .collect(Collectors.groupingBy(p -> p.getAlocacao().getFuncionario().getId(), Collectors.counting()));

        List<FuncionarioRankingDTO> maisPresentes = contagemPresencasPorFunc.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Funcionario f = funcionarioRepository.findById(e.getKey()).orElse(null);
                    return new FuncionarioRankingDTO(e.getKey(), f != null ? f.getNome() : "Desconhecido", e.getValue());
                })
                .toList();

        // Ranking mais ausentes
        Map<Integer, Long> contagemAusenciasPorFunc = presencas.stream()
                .filter(p -> p.getStatus() == StatusPresenca.AUSENTE || p.getStatus() == StatusPresenca.JUSTIFICADA)
                .collect(Collectors.groupingBy(p -> p.getAlocacao().getFuncionario().getId(), Collectors.counting()));

        List<FuncionarioRankingDTO> maisAusentes = contagemAusenciasPorFunc.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Funcionario f = funcionarioRepository.findById(e.getKey()).orElse(null);
                    return new FuncionarioRankingDTO(e.getKey(), f != null ? f.getNome() : "Desconhecido", e.getValue());
                })
                .toList();

        // Datas de maior ausência
        Map<String, Long> datasMaiorAusencia = presencas.stream()
                .filter(p -> p.getStatus() == StatusPresenca.AUSENTE || p.getStatus() == StatusPresenca.JUSTIFICADA)
                .collect(Collectors.groupingBy(p -> p.getAlocacao().getData().toString(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return new RelatorioPresencaResponseDTO(
                totalRegistros,
                totalPresencas,
                totalAusencias,
                porcentagemPresenca,
                maisPresentes,
                maisAusentes,
                datasMaiorAusencia
        );
    }

    // ==========================================
    // 3. Férias e Afastamentos (RF-FU10, RF-FU11)
    // ==========================================

    @Transactional
    public FeriasResponseDTO registrarFerias(FeriasRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Os dados de férias são obrigatórios.");
        }
        if (dto.dataInicio().isAfter(dto.dataFim())) {
            throw new IllegalArgumentException("A data de início das férias não pode ser posterior à data final.");
        }

        Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado com ID: " + dto.funcionarioId()));

        List<Ferias> conflitos = feriasRepository.findConflitantes(funcionario.getId(), dto.dataInicio(), dto.dataFim());
        if (!conflitos.isEmpty()) {
            throw new IllegalArgumentException("O funcionário já possui férias cadastradas que coincidem com este período.");
        }

        Ferias ferias = new Ferias();
        ferias.setFuncionario(funcionario);
        ferias.setDataInicio(dto.dataInicio());
        ferias.setDataFim(dto.dataFim());
        ferias.setTipo(dto.tipo());

        Ferias salvas = feriasRepository.save(ferias);
        return new FeriasResponseDTO(salvas);
    }

    @Transactional(readOnly = true)
    public List<FeriasResponseDTO> listarFeriasPorFuncionario(Integer funcionarioId) {
        return feriasRepository.findByFuncionarioId(funcionarioId)
                .stream()
                .map(FeriasResponseDTO::new)
                .toList();
    }

    @Transactional
    public AfastamentoResponseDTO registrarAfastamento(AfastamentoRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Os dados de afastamento são obrigatórios.");
        }
        if (dto.dataInicio().isAfter(dto.dataFim())) {
            throw new IllegalArgumentException("A data de início do afastamento não pode ser posterior à data final.");
        }

        Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado com ID: " + dto.funcionarioId()));

        List<Afastamento> conflitos = afastamentoRepository.findConflitantes(funcionario.getId(), dto.dataInicio(), dto.dataFim());
        if (!conflitos.isEmpty()) {
            throw new IllegalArgumentException("O funcionário já possui afastamento cadastrado que coincide com este período.");
        }

        Afastamento afastamento = new Afastamento();
        afastamento.setFuncionario(funcionario);
        afastamento.setDataInicio(dto.dataInicio());
        afastamento.setDataFim(dto.dataFim());
        afastamento.setTipo(dto.tipo());

        Afastamento salvo = afastamentoRepository.save(afastamento);
        return new AfastamentoResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<AfastamentoResponseDTO> listarAfastamentosPorFuncionario(Integer funcionarioId) {
        return afastamentoRepository.findByFuncionarioId(funcionarioId)
                .stream()
                .map(AfastamentoResponseDTO::new)
                .toList();
    }

    // ==========================================
    // 4. Consulta de Disponibilidade (RF-FU12)
    // ==========================================

    @Transactional(readOnly = true)
    public DisponibilidadeResponseDTO consultarDisponibilidade(Integer funcionarioId, LocalDate data) {
        if (funcionarioId == null || data == null) {
            throw new IllegalArgumentException("O ID do funcionário e a data são obrigatórios.");
        }

        Funcionario f = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado com ID: " + funcionarioId));

        if (!f.isAtivo()) {
            return new DisponibilidadeResponseDTO(f.getId(), f.getNome(), data, false, "Funcionário está INATIVO.");
        }

        if (feriasRepository.isFuncionarioEmFerias(f.getId(), data)) {
            return new DisponibilidadeResponseDTO(f.getId(), f.getNome(), data, false, "Funcionário está em período de FÉRIAS.");
        }

        if (afastamentoRepository.isFuncionarioAfastado(f.getId(), data)) {
            return new DisponibilidadeResponseDTO(f.getId(), f.getNome(), data, false, "Funcionário está em período de AFASTAMENTO.");
        }

        List<Alocacao> alocacoes = alocacaoRepository.findAtivasByFuncionarioIdAndData(f.getId(), data);
        if (!alocacoes.isEmpty()) {
            String turnos = alocacoes.stream().map(a -> a.getTurno().getNome()).collect(Collectors.joining(", "));
            return new DisponibilidadeResponseDTO(f.getId(), f.getNome(), data, false, "Funcionário já alocado no(s) turno(s): " + turnos);
        }

        return new DisponibilidadeResponseDTO(f.getId(), f.getNome(), data, true, "Disponível para alocação.");
    }
}
