package br.ufs.dcomp.collina.service.espacos;

import br.ufs.dcomp.collina.dto.espacos.ReservaRequestDTO;
import br.ufs.dcomp.collina.dto.espacos.ReservaResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.associados.Associado;
import br.ufs.dcomp.collina.model.associados.SituacaoCadastralEnum;
import br.ufs.dcomp.collina.model.associados.StatusVinculoEnum;
import br.ufs.dcomp.collina.model.associados.VinculoDependencia;
import br.ufs.dcomp.collina.model.auth.PerfilUsuarioEnum;
import br.ufs.dcomp.collina.model.espacos.BlocoHorario;
import br.ufs.dcomp.collina.model.espacos.EspacoFisico;
import br.ufs.dcomp.collina.model.espacos.EstadoEspacoEnum;
import br.ufs.dcomp.collina.model.espacos.Reserva;
import br.ufs.dcomp.collina.model.espacos.StatusReservaEnum;
import br.ufs.dcomp.collina.model.espacos.TipoBlocoEnum;
import br.ufs.dcomp.collina.repository.associados.VinculoDependenciaRepository;
import br.ufs.dcomp.collina.repository.espacos.AssociadoReservaRepository;
import br.ufs.dcomp.collina.repository.espacos.BlocoHorarioRepository;
import br.ufs.dcomp.collina.repository.espacos.EspacoFisicoRepository;
import br.ufs.dcomp.collina.repository.espacos.ReservaRepository;
import br.ufs.dcomp.collina.service.associados.AssociadoService;
import br.ufs.dcomp.collina.service.associados.VinculoDependenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// UC-GE02 / UC-ASSO07 (solicitar), UC-GE03 / UC-ASSO08 (cancelar) e UC-ASSO09 (consultar).
// Toda a regra de reserva fica concentrada aqui e em ReservaRegras (INC02).
@Service
public class ReservaService {

    private final AssociadoService associadoService;
    private final AssociadoReservaRepository associadoReservaRepository;
    private final VinculoDependenciaRepository vinculoRepository;
    private final EspacoFisicoRepository espacoRepository;
    private final BlocoHorarioRepository blocoRepository;
    private final ReservaRepository reservaRepository;
    private final ConsultaAdimplencia consultaAdimplencia;
    private final Clock clock;
    private final int maxDiasAntecedencia;

    @Autowired
    public ReservaService(AssociadoService associadoService,
                          AssociadoReservaRepository associadoReservaRepository,
                          VinculoDependenciaRepository vinculoRepository,
                          EspacoFisicoRepository espacoRepository,
                          BlocoHorarioRepository blocoRepository,
                          ReservaRepository reservaRepository,
                          ConsultaAdimplencia consultaAdimplencia,
                          @Value("${collina.reservas.max-dias-antecedencia:7}") int maxDiasAntecedencia) {
        this(associadoService, associadoReservaRepository, vinculoRepository, espacoRepository,
                blocoRepository, reservaRepository, consultaAdimplencia,
                Clock.system(ReservaRegras.FUSO), maxDiasAntecedencia);
    }

    // Usado nos testes (relógio fixo)
    ReservaService(AssociadoService associadoService,
                   AssociadoReservaRepository associadoReservaRepository,
                   VinculoDependenciaRepository vinculoRepository,
                   EspacoFisicoRepository espacoRepository,
                   BlocoHorarioRepository blocoRepository,
                   ReservaRepository reservaRepository,
                   ConsultaAdimplencia consultaAdimplencia,
                   Clock clock,
                   int maxDiasAntecedencia) {
        this.associadoService = associadoService;
        this.associadoReservaRepository = associadoReservaRepository;
        this.vinculoRepository = vinculoRepository;
        this.espacoRepository = espacoRepository;
        this.blocoRepository = blocoRepository;
        this.reservaRepository = reservaRepository;
        this.consultaAdimplencia = consultaAdimplencia;
        this.clock = clock;
        this.maxDiasAntecedencia = maxDiasAntecedencia > 0
                ? maxDiasAntecedencia : ReservaRegras.MAX_DIAS_ANTECEDENCIA_PADRAO;
    }

    // ---------------------------------------------------------------- solicitar

    // RF-AS08: só o titular reserva. Todos os blocos são validados antes de gravar qualquer um, e a
    // transação garante o "tudo ou nada".
    @Transactional
    public List<ReservaResponseDTO> solicitar(String usuarioUid, ReservaRequestDTO dto) {
        Integer titularId = associadoService.obterIdPorUsuarioId(usuarioUid);

        LocalDate hoje = LocalDate.now(clock);
        ReservaRegras.validarAntecedencia(dto.data(), hoje, maxDiasAntecedencia);

        Set<Integer> blocoIds = new LinkedHashSet<>(dto.blocoIds());
        if (blocoIds.size() != dto.blocoIds().size()) {
            throw new IllegalArgumentException("Há blocos de horário repetidos na solicitação.");
        }
        int qtdConvidados = dto.qtdConvidados() == null ? 0 : dto.qtdConvidados();

        // Trava o titular (cota mensal) e depois o espaço (disputa de horários), sempre nessa ordem
        Associado titular = associadoReservaRepository.findWithLockById(titularId)
                .orElseThrow(() -> new ResourceNotFoundException("Associado titular não encontrado."));
        validarTitular(titular);

        EspacoFisico espaco = espacoRepository.findWithLockById(dto.espacoId())
                .orElseThrow(() -> new ResourceNotFoundException("Espaço não encontrado."));
        validarEspacoReservavel(espaco);

        List<BlocoHorario> blocos = blocoRepository.findByEspacoIdAndIdIn(espaco.getId(), blocoIds);
        if (blocos.size() != blocoIds.size()) {
            throw new ResourceNotFoundException("Um ou mais blocos de horário não pertencem ao espaço informado.");
        }
        if (blocos.stream().anyMatch(b -> b.getTipo() != TipoBlocoEnum.LIVRE)) {
            throw new IllegalStateException(
                    "Há bloco reservado para treino ou manutenção; escolha apenas horários livres.");
        }

        VinculoDependencia dependente = dto.dependenteId() == null
                ? null : validarDependente(dto.dependenteId(), titularId, dto.data());

        ReservaRegras.validarConvidados(espaco.getLimiteConvidados(), qtdConvidados);
        ReservaRegras.validarOcupacao(espaco.isPublico(), espaco.getCapacidadeMaxima(),
                dependente != null, qtdConvidados);

        if (!reservaRepository.findByEspacoIdAndDataUsoAndStatusAndBlocoIdIn(
                espaco.getId(), dto.data(), StatusReservaEnum.CONFIRMADA, blocoIds).isEmpty()) {
            throw new IllegalStateException("Um ou mais horários selecionados não estão mais disponíveis.");
        }

        LocalDate inicioMes = dto.data().withDayOfMonth(1);
        long jaReservadas = reservaRepository.countByTitularIdAndStatusAndDataUsoGreaterThanEqualAndDataUsoLessThan(
                titularId, StatusReservaEnum.CONFIRMADA, inicioMes, inicioMes.plusMonths(1));
        ReservaRegras.validarCota(jaReservadas, blocos.size(), titular.getPlano().getLimiteMensalReservas());

        Instant agora = clock.instant();
        return blocos.stream()
                .sorted(Comparator.comparing(BlocoHorario::getHoraInicio))
                .map(bloco -> gravar(titular, espaco, bloco, dependente, dto.data(), qtdConvidados, agora))
                .map(ReservaResponseDTO::new)
                .toList();
    }

    private Reserva gravar(Associado titular, EspacoFisico espaco, BlocoHorario bloco, VinculoDependencia dependente,
                           LocalDate data, int qtdConvidados, Instant agora) {
        Reserva reserva = new Reserva();
        reserva.setTitular(titular);
        reserva.setEspaco(espaco);
        reserva.setBloco(bloco);
        reserva.setDependente(dependente);
        reserva.setDataUso(data);
        reserva.setQtdConvidados(qtdConvidados);
        reserva.setStatus(StatusReservaEnum.CONFIRMADA);
        reserva.setCriadaEm(agora);
        try {
            return reservaRepository.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException e) {
            // Rede de segurança: a restrição única do banco barrou uma reserva simultânea
            throw new IllegalStateException("Um ou mais horários selecionados não estão mais disponíveis.");
        }
    }

    private void validarTitular(Associado titular) {
        if (titular.getSituacaoCadastral() != SituacaoCadastralEnum.ATIVO) {
            throw new IllegalStateException("Reserva não permitida: situação cadastral do associado é "
                    + titular.getSituacaoCadastral().name().toLowerCase() + ".");
        }
        // RF-AS07
        if (consultaAdimplencia.possuiMensalidadeEmAtraso(titular.getId())) {
            throw new IllegalStateException("Reserva bloqueada por inadimplência: há mensalidade em atraso.");
        }
    }

    private void validarEspacoReservavel(EspacoFisico espaco) {
        if (espaco.getEstado() != EstadoEspacoEnum.ATIVO) {
            throw new IllegalStateException(
                    "O espaço não está disponível para reservas (em manutenção ou inativo).");
        }
        if (espaco.isUsoExclusivoTreino()) {
            throw new IllegalStateException("Este espaço é de uso exclusivo para treino e não aceita reservas.");
        }
    }

    // RF-AS03 / RF-AS04 / RF-AS08
    private VinculoDependencia validarDependente(Integer vinculoId, Integer titularId, LocalDate dataUso) {
        VinculoDependencia vinculo = vinculoRepository.findById(vinculoId).orElse(null);
        if (vinculo == null || !vinculo.getTitular().getId().equals(titularId)) {
            throw new IllegalStateException("O dependente informado não está vinculado ao seu cadastro.");
        }
        if (vinculo.getStatus() != StatusVinculoEnum.ATIVO) {
            throw new IllegalStateException("O vínculo de dependência está inativo.");
        }
        int idade = vinculo.getDependente().idadeEm(dataUso);
        if (!ReservaRegras.dependenteElegivel(idade, VinculoDependenciaService.IDADE_LIMITE_DEPENDENTE)) {
            throw new IllegalStateException("O dependente não é elegível: completa "
                    + VinculoDependenciaService.IDADE_LIMITE_DEPENDENTE + " anos até a data de uso.");
        }
        return vinculo;
    }

    // ----------------------------------------------------------------- cancelar

    // RF-AS09: até o dia anterior, sem penalidade. Titular cancela só as suas; recepção/admin cancelam
    // em nome do titular (a permissão por perfil está no SecurityConfig).
    @Transactional
    public ReservaResponseDTO cancelar(PerfilUsuarioEnum perfil, String usuarioUid, Integer reservaId) {
        Reserva reserva = reservaRepository.findWithLockById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada."));

        if (perfil == PerfilUsuarioEnum.ASSOCIADO) {
            Integer meuId = associadoService.obterIdPorUsuarioId(usuarioUid);
            if (!reserva.getTitular().getId().equals(meuId)) {
                throw new AccessDeniedException("Você só pode cancelar as suas próprias reservas.");
            }
        }
        if (reserva.getStatus() != StatusReservaEnum.CONFIRMADA) {
            throw new IllegalStateException("A reserva já está cancelada.");
        }
        ReservaRegras.validarPrazoCancelamento(reserva.getDataUso(), LocalDate.now(clock));

        reserva.cancelar(clock.instant());
        return new ReservaResponseDTO(reservaRepository.save(reserva));
    }

    // ----------------------------------------------------------------- consultar

    // UC-ASSO09: o titular vê as suas (inclui canceladas). Não é bloqueado por inadimplência (RF-AS11).
    @Transactional(readOnly = true)
    public List<ReservaResponseDTO> listarMinhas(String usuarioUid) {
        return listarDoTitular(associadoService.obterIdPorUsuarioId(usuarioUid));
    }

    // Recepção e administrador consultam por associado
    @Transactional(readOnly = true)
    public List<ReservaResponseDTO> listarPorAssociado(Integer associadoId) {
        associadoService.buscarPorId(associadoId); // 404 se o associado não existir
        return listarDoTitular(associadoId);
    }

    private List<ReservaResponseDTO> listarDoTitular(Integer titularId) {
        return reservaRepository.findByTitularId(titularId).stream()
                .sorted(Comparator.comparing(Reserva::getDataUso).reversed()
                        .thenComparing(r -> r.getBloco().getHoraInicio()))
                .map(ReservaResponseDTO::new)
                .toList();
    }
}
