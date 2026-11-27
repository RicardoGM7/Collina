package br.ufs.dcomp.collina.service.funcionarios;

import br.ufs.dcomp.collina.dto.funcionarios.*;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.funcionarios.*;
import br.ufs.dcomp.collina.repository.funcionarios.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
public class EscalaService {

    @Autowired
    private EscalaRepository escalaRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private AlocacaoRepository alocacaoRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private FeriasRepository feriasRepository;

    @Autowired
    private AfastamentoRepository afastamentoRepository;

    // ==========================================
    // 1. Gestão de Turnos (RF-FU06)
    // ==========================================

    @Transactional
    public TurnoResponseDTO criarTurno(TurnoRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Os dados do turno são obrigatórios.");
        }
        if (turnoRepository.existsByNomeIgnoreCase(dto.nome().trim())) {
            throw new IllegalArgumentException("Já existe um turno cadastrado com o nome: " + dto.nome());
        }

        Turno turno = new Turno();
        turno.setNome(dto.nome().trim());
        turno.setHoraInicio(dto.horaInicio());
        turno.setHoraFim(dto.horaFim());

        Turno salvo = turnoRepository.save(turno);
        return new TurnoResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<TurnoResponseDTO> listarTurnos() {
        return turnoRepository.findAll()
                .stream()
                .map(TurnoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public TurnoResponseDTO buscarTurnoPorId(Integer id) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno não encontrado com ID: " + id));
        return new TurnoResponseDTO(turno);
    }

    @Transactional
    public TurnoResponseDTO editarTurno(Integer id, TurnoRequestDTO dto) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno não encontrado com ID: " + id));

        if (!turno.getNome().equalsIgnoreCase(dto.nome().trim()) && turnoRepository.existsByNomeIgnoreCase(dto.nome().trim())) {
            throw new IllegalArgumentException("Já existe outro turno cadastrado com o nome: " + dto.nome());
        }

        turno.setNome(dto.nome().trim());
        turno.setHoraInicio(dto.horaInicio());
        turno.setHoraFim(dto.horaFim());

        Turno atualizado = turnoRepository.save(turno);
        return new TurnoResponseDTO(atualizado);
    }

    // ==========================================
    // 2. Gestão de Escalas (RF-FU05)
    // ==========================================

    @Transactional
    public EscalaResponseDTO criarEscala(EscalaRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Os dados da escala são obrigatórios.");
        }
        if (escalaRepository.existsByPeriodo(dto.periodo().trim())) {
            throw new IllegalArgumentException("Já existe uma escala cadastrada para o período: " + dto.periodo());
        }

        Escala escala = new Escala();
        escala.setPeriodo(dto.periodo().trim());
        escala.setStatus(StatusEscala.PENDENTE);

        Escala salva = escalaRepository.save(escala);
        return new EscalaResponseDTO(salva);
    }

    @Transactional(readOnly = true)
    public List<EscalaResponseDTO> listarEscalas() {
        return escalaRepository.findAll()
                .stream()
                .map(EscalaResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public EscalaResponseDTO buscarEscalaPorId(Integer id) {
        Escala escala = escalaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escala não encontrada com ID: " + id));
        return new EscalaResponseDTO(escala);
    }

    @Transactional
    public EscalaResponseDTO editarEscala(Integer id, EscalaRequestDTO dto) {
        Escala escala = escalaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escala não encontrada com ID: " + id));

        if (escala.getStatus() == StatusEscala.PUBLICADA) {
            throw new IllegalStateException("Não é permitido alterar o período de uma escala já publicada.");
        }

        if (!escala.getPeriodo().equalsIgnoreCase(dto.periodo().trim()) && escalaRepository.existsByPeriodo(dto.periodo().trim())) {
            throw new IllegalArgumentException("Já existe outra escala cadastrada para o período: " + dto.periodo());
        }

        escala.setPeriodo(dto.periodo().trim());
        Escala atualizada = escalaRepository.save(escala);
        return new EscalaResponseDTO(atualizada);
    }

    @Transactional
    public EscalaResponseDTO publicarEscala(Integer id) {
        Escala escala = escalaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escala não encontrada com ID: " + id));

        if (escala.getStatus() == StatusEscala.PUBLICADA) {
            throw new IllegalStateException("A escala já está publicada.");
        }
        if (escala.getStatus() == StatusEscala.CANCELADA) {
            throw new IllegalStateException("Não é possível publicar uma escala cancelada.");
        }

        escala.setStatus(StatusEscala.PUBLICADA);

        // Confirma as alocações pendentes da escala
        if (escala.getAlocacoes() != null) {
            for (Alocacao alocacao : escala.getAlocacoes()) {
                if (alocacao.getStatus() == StatusAlocacao.PENDENTE) {
                    alocacao.setStatus(StatusAlocacao.CONFIRMADA);
                }
            }
        }

        Escala publicada = escalaRepository.save(escala);
        return new EscalaResponseDTO(publicada);
    }

    // ==========================================
    // 3. Validação e Alocação de Funcionários (RF-FU13 / RD-03 / CLT)
    // ==========================================

    @Transactional(readOnly = true)
    public ValidacaoAlocacaoResponseDTO validarAlocacao(AlocacaoRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Os dados de validação da alocação são obrigatórios.");
        }

        List<String> motivos = new ArrayList<>();

        Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                .orElse(null);
        if (funcionario == null) {
            motivos.add("Funcionário não encontrado com ID: " + dto.funcionarioId());
            return new ValidacaoAlocacaoResponseDTO(false, motivos, 0.0, 0.0);
        }

        // RF-FU07: Funcionários com status inativo não podem ser alocados na escala
        if (!funcionario.isAtivo()) {
            motivos.add("Funcionário está com status INATIVO e não pode ser alocado.");
        }

        Turno novoTurno = turnoRepository.findById(dto.turnoId()).orElse(null);
        if (novoTurno == null) {
            motivos.add("Turno não encontrado com ID: " + dto.turnoId());
            return new ValidacaoAlocacaoResponseDTO(false, motivos, 0.0, 0.0);
        }

        Escala escala = escalaRepository.findById(dto.escalaId()).orElse(null);
        if (escala == null) {
            motivos.add("Escala não encontrada com ID: " + dto.escalaId());
        } else if (escala.getStatus() == StatusEscala.CANCELADA) {
            motivos.add("A escala informada está CANCELADA.");
        }

        LocalDate data = dto.data();
        if (data == null) {
            motivos.add("A data da alocação é obrigatória.");
            return new ValidacaoAlocacaoResponseDTO(false, motivos, 0.0, 0.0);
        }

        // RF-FU10: Férias
        if (feriasRepository.isFuncionarioEmFerias(funcionario.getId(), data)) {
            motivos.add("O funcionário está em período de FÉRIAS na data informada (" + data + ").");
        }

        // RF-FU11: Afastamento
        if (afastamentoRepository.isFuncionarioAfastado(funcionario.getId(), data)) {
            motivos.add("O funcionário está em período de AFASTAMENTO na data informada (" + data + ").");
        }

        // Alocações do dia já existentes para o funcionário
        List<Alocacao> alocacoesDoDia = alocacaoRepository.findAtivasByFuncionarioIdAndData(funcionario.getId(), data);

        double horasDoDia = novoTurno.getDuracaoHoras();
        for (Alocacao aloc : alocacoesDoDia) {
            horasDoDia += aloc.getTurno().getDuracaoHoras();

            // Conflito direto de turno
            if (aloc.getTurno().getId().equals(novoTurno.getId())) {
                motivos.add("O funcionário já está alocado para o turno '" + novoTurno.getNome() + "' nesta data.");
            }
        }

        // CLT / RD-03: Limite diário de até 8 horas
        if (horasDoDia > 8.0) {
            motivos.add(String.format("Limite diário de 8 horas da CLT excedido: total acumulado seria de %.1fh no dia %s.", horasDoDia, data));
        }

        // CLT / RD-03: Limite semanal de 44 horas
        LocalDate inicioSemana = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fimSemana = data.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        List<Alocacao> alocacoesSemana = alocacaoRepository.findAtivasByFuncionarioIdAndPeriodo(funcionario.getId(), inicioSemana, fimSemana);

        double horasSemana = novoTurno.getDuracaoHoras();
        for (Alocacao aloc : alocacoesSemana) {
            horasSemana += aloc.getTurno().getDuracaoHoras();
        }

        if (horasSemana > 44.0) {
            motivos.add(String.format("Limite semanal de 44 horas da CLT excedido: total acumulado seria de %.1fh na semana (%s a %s).",
                    horasSemana, inicioSemana, fimSemana));
        }

        // CLT / RD-03: Intervalo interjornada de pelo menos 11 horas de descanso
        LocalDateTime novoInicio = LocalDateTime.of(data, novoTurno.getHoraInicio());
        LocalDateTime novoFim = LocalDateTime.of(data, novoTurno.getHoraFim());
        if (!novoTurno.getHoraFim().isAfter(novoTurno.getHoraInicio())) {
            novoFim = novoFim.plusDays(1); // Turno noturno que vira a noite
        }

        // Checa alocações do dia anterior e posterior para garantir 11 horas
        List<Alocacao> alocacoesProximas = alocacaoRepository.findAtivasByFuncionarioIdAndPeriodo(
                funcionario.getId(), data.minusDays(1), data.plusDays(1));

        for (Alocacao aloc : alocacoesProximas) {
            LocalDateTime inicioAloc = LocalDateTime.of(aloc.getData(), aloc.getTurno().getHoraInicio());
            LocalDateTime fimAloc = LocalDateTime.of(aloc.getData(), aloc.getTurno().getHoraFim());
            if (!aloc.getTurno().getHoraFim().isAfter(aloc.getTurno().getHoraInicio())) {
                fimAloc = fimAloc.plusDays(1);
            }

            // Se a alocação existente termina antes do novo início, descanso = entre fimAloc e novoInicio
            if (!fimAloc.isAfter(novoInicio)) {
                long horasDescanso = ChronoUnit.HOURS.between(fimAloc, novoInicio);
                if (horasDescanso < 11) {
                    motivos.add(String.format("Descanso interjornada inferior a 11 horas: apenas %dh de intervalo após o turno do dia %s.",
                            horasDescanso, aloc.getData()));
                }
            }
            // Se a nova alocação termina antes do início da existente, descanso = entre novoFim e inicioAloc
            else if (!novoFim.isAfter(inicioAloc)) {
                long horasDescanso = ChronoUnit.HOURS.between(novoFim, inicioAloc);
                if (horasDescanso < 11) {
                    motivos.add(String.format("Descanso interjornada inferior a 11 horas: apenas %dh de intervalo antes do turno do dia %s.",
                            horasDescanso, aloc.getData()));
                }
            }
        }

        boolean valido = motivos.isEmpty();
        return new ValidacaoAlocacaoResponseDTO(valido, motivos, horasDoDia, horasSemana);
    }

    @Transactional
    public AlocacaoResponseDTO alocarFuncionario(AlocacaoRequestDTO dto) {
        ValidacaoAlocacaoResponseDTO validacao = validarAlocacao(dto);
        if (!validacao.valido()) {
            throw new IllegalArgumentException("Alocação inválida: " + String.join(" | ", validacao.motivos()));
        }

        Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado com ID: " + dto.funcionarioId()));

        Escala escala = escalaRepository.findById(dto.escalaId())
                .orElseThrow(() -> new ResourceNotFoundException("Escala não encontrada com ID: " + dto.escalaId()));

        Turno turno = turnoRepository.findById(dto.turnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Turno não encontrado com ID: " + dto.turnoId()));

        Alocacao alocacao = new Alocacao();
        alocacao.setFuncionario(funcionario);
        alocacao.setEscala(escala);
        alocacao.setTurno(turno);
        alocacao.setData(dto.data());
        alocacao.setStatus(escala.getStatus() == StatusEscala.PUBLICADA ? StatusAlocacao.CONFIRMADA : StatusAlocacao.PENDENTE);

        Alocacao salva = alocacaoRepository.save(alocacao);
        return new AlocacaoResponseDTO(salva);
    }

    @Transactional(readOnly = true)
    public List<AlocacaoResponseDTO> listarAlocacoesPorEscala(Integer escalaId) {
        return alocacaoRepository.findByEscalaId(escalaId)
                .stream()
                .map(AlocacaoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlocacaoResponseDTO> listarAlocacoesPorFuncionario(Integer funcionarioId) {
        return alocacaoRepository.findByFuncionarioId(funcionarioId)
                .stream()
                .map(AlocacaoResponseDTO::new)
                .toList();
    }

    @Transactional
    public AlocacaoResponseDTO atualizarStatusAlocacao(Integer id, StatusAlocacao novoStatus) {
        if (novoStatus == null) {
            throw new IllegalArgumentException("O novo status da alocação é obrigatório.");
        }
        Alocacao alocacao = alocacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alocação não encontrada com ID: " + id));

        alocacao.setStatus(novoStatus);
        Alocacao atualizada = alocacaoRepository.save(alocacao);
        return new AlocacaoResponseDTO(atualizada);
    }

    @Transactional
    public void cancelarAlocacao(Integer id) {
        Alocacao alocacao = alocacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alocação não encontrada com ID: " + id));

        alocacao.setStatus(StatusAlocacao.CANCELADA);
        alocacaoRepository.save(alocacao);
    }
}
