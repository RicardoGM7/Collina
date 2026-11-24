package br.ufs.dcomp.collina.service.espacos;

import br.ufs.dcomp.collina.dto.espacos.BlocoRequestDTO;
import br.ufs.dcomp.collina.dto.espacos.BlocoResponseDTO;
import br.ufs.dcomp.collina.dto.espacos.DisponibilidadeResponseDTO;
import br.ufs.dcomp.collina.dto.espacos.DisponibilidadeResponseDTO.BlocoDisponibilidadeDTO;
import br.ufs.dcomp.collina.dto.espacos.EspacoRequestDTO;
import br.ufs.dcomp.collina.dto.espacos.EspacoResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.auth.PerfilUsuarioEnum;
import br.ufs.dcomp.collina.model.espacos.BlocoHorario;
import br.ufs.dcomp.collina.model.espacos.EspacoFisico;
import br.ufs.dcomp.collina.model.espacos.EstadoEspacoEnum;
import br.ufs.dcomp.collina.model.espacos.Reserva;
import br.ufs.dcomp.collina.model.espacos.SituacaoBlocoEnum;
import br.ufs.dcomp.collina.model.espacos.StatusReservaEnum;
import br.ufs.dcomp.collina.model.espacos.TipoBlocoEnum;
import br.ufs.dcomp.collina.repository.espacos.BlocoHorarioRepository;
import br.ufs.dcomp.collina.repository.espacos.EspacoFisicoRepository;
import br.ufs.dcomp.collina.repository.espacos.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// UC-GE01 (disponibilidade), UC-GE04 (espaços) e UC-GE05 (blocos de horário).
// Quem pode chamar cada operação é definido no SecurityConfig; aqui só o que depende do perfil na resposta.
@Service
public class EspacoService {

    private final EspacoFisicoRepository espacoRepository;
    private final BlocoHorarioRepository blocoRepository;
    private final ReservaRepository reservaRepository;

    @Autowired
    public EspacoService(EspacoFisicoRepository espacoRepository,
                         BlocoHorarioRepository blocoRepository,
                         ReservaRepository reservaRepository) {
        this.espacoRepository = espacoRepository;
        this.blocoRepository = blocoRepository;
        this.reservaRepository = reservaRepository;
    }

    // ------------------------------------------------------------------ espaços

    // Associado vê só os espaços reserváveis (ativos e que não são de uso exclusivo para treino)
    @Transactional(readOnly = true)
    public List<EspacoResponseDTO> listar(PerfilUsuarioEnum perfil) {
        return espacoRepository.findAllByOrderByNomeAsc().stream()
                .filter(e -> perfil != PerfilUsuarioEnum.ASSOCIADO || e.isReservavel())
                .map(EspacoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public EspacoResponseDTO buscarPorId(PerfilUsuarioEnum perfil, Integer id) {
        EspacoFisico espaco = obterEspaco(id);
        if (perfil == PerfilUsuarioEnum.ASSOCIADO && !espaco.isReservavel()) {
            throw new ResourceNotFoundException("Espaço não encontrado.");
        }
        return new EspacoResponseDTO(espaco);
    }

    @Transactional
    public EspacoResponseDTO cadastrar(EspacoRequestDTO dto) {
        EspacoFisico espaco = new EspacoFisico();
        String nome = dto.nome().trim();
        if (espacoRepository.existsByNomeIgnoreCase(nome)) {
            throw new IllegalStateException("Já existe um espaço com esse nome.");
        }
        aplicar(espaco, nome, dto);
        return new EspacoResponseDTO(espacoRepository.save(espaco));
    }

    @Transactional
    public EspacoResponseDTO atualizar(Integer id, EspacoRequestDTO dto) {
        EspacoFisico espaco = obterEspaco(id);
        String nome = dto.nome().trim();
        if (espacoRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new IllegalStateException("Já existe um espaço com esse nome.");
        }
        aplicar(espaco, nome, dto);
        return new EspacoResponseDTO(espacoRepository.save(espaco));
    }

    // UC-GE04, fluxo alternativo: manutenção/inativo bloqueia NOVAS reservas; as já confirmadas permanecem
    @Transactional
    public EspacoResponseDTO alterarEstado(Integer id, EstadoEspacoEnum estado) {
        EspacoFisico espaco = obterEspaco(id);
        espaco.setEstado(estado);
        return new EspacoResponseDTO(espacoRepository.save(espaco));
    }

    private void aplicar(EspacoFisico espaco, String nome, EspacoRequestDTO dto) {
        boolean publico = Boolean.TRUE.equals(dto.publico());
        ReservaRegras.validarEspaco(publico, dto.limiteConvidados(), dto.capacidadeMaxima());
        espaco.setNome(nome);
        espaco.setPublico(publico);
        espaco.setUsoExclusivoTreino(Boolean.TRUE.equals(dto.usoExclusivoTreino()));
        espaco.setLimiteConvidados(dto.limiteConvidados());
        // Espaço público não tem controle de ocupação: a capacidade é descartada
        espaco.setCapacidadeMaxima(publico ? null : dto.capacidadeMaxima());
    }

    // ------------------------------------------------------------------- blocos

    @Transactional(readOnly = true)
    public List<BlocoResponseDTO> listarBlocos(PerfilUsuarioEnum perfil, Integer espacoId) {
        buscarPorId(perfil, espacoId); // 404 se não existir ou se o associado não puder ver
        return blocoRepository.findByEspacoIdOrderByHoraInicioAsc(espacoId).stream()
                .map(BlocoResponseDTO::new)
                .toList();
    }

    // UC-GE05 (somente administrador, definido no SecurityConfig)
    @Transactional
    public BlocoResponseDTO adicionarBloco(Integer espacoId, BlocoRequestDTO dto) {
        EspacoFisico espaco = espacoRepository.findWithLockById(espacoId)
                .orElseThrow(() -> new ResourceNotFoundException("Espaço não encontrado."));

        LocalTime inicio = LocalTime.parse(dto.horaInicio());
        LocalTime fim = LocalTime.parse(dto.horaFim());
        ReservaRegras.validarDuracaoBloco(inicio, fim);

        for (BlocoHorario existente : blocoRepository.findByEspacoIdOrderByHoraInicioAsc(espacoId)) {
            if (ReservaRegras.blocosSeSobrepoem(inicio, fim, existente.getHoraInicio(), existente.getHoraFim())) {
                throw new IllegalStateException("O bloco se sobrepõe a um bloco já configurado para este espaço.");
            }
        }

        BlocoHorario bloco = new BlocoHorario();
        bloco.setEspaco(espaco);
        bloco.setHoraInicio(inicio);
        bloco.setHoraFim(fim);
        bloco.setTipo(dto.tipo() != null ? dto.tipo() : TipoBlocoEnum.LIVRE);
        return new BlocoResponseDTO(blocoRepository.save(bloco));
    }

    // ---------------------------------------------------------- disponibilidade

    // RF-ESP03 / UC-GE01
    @Transactional(readOnly = true)
    public DisponibilidadeResponseDTO consultarDisponibilidade(PerfilUsuarioEnum perfil, Integer espacoId, LocalDate data) {
        if (data == null) {
            throw new IllegalArgumentException("A data é obrigatória (formato AAAA-MM-DD).");
        }
        EspacoFisico espaco = obterEspaco(espacoId);
        boolean associado = perfil == PerfilUsuarioEnum.ASSOCIADO;

        if (associado && !espaco.isReservavel()) {
            return new DisponibilidadeResponseDTO(espacoId, data, List.of(),
                    "Não há horários livres para o espaço selecionado.");
        }

        Set<Integer> ocupados = new HashSet<>();
        for (Reserva r : reservaRepository.findByEspacoIdAndDataUsoAndStatus(
                espacoId, data, StatusReservaEnum.CONFIRMADA)) {
            ocupados.add(r.getBloco().getId());
        }

        List<BlocoDisponibilidadeDTO> agenda = blocoRepository.findByEspacoIdOrderByHoraInicioAsc(espacoId).stream()
                .map(b -> new BlocoDisponibilidadeDTO(
                        b.getId(),
                        b.getHoraInicio().format(BlocoResponseDTO.HORA),
                        b.getHoraFim().format(BlocoResponseDTO.HORA),
                        b.getTipo(),
                        situacao(espaco, b, ocupados)))
                .toList();

        // Associado só enxerga o que pode reservar (oculta ocupados, treino e manutenção)
        List<BlocoDisponibilidadeDTO> visiveis = associado
                ? agenda.stream().filter(b -> b.situacao() == SituacaoBlocoEnum.LIVRE).toList()
                : agenda;

        String mensagem = visiveis.isEmpty() ? "Não há horários livres para o espaço selecionado." : null;
        return new DisponibilidadeResponseDTO(espacoId, data, visiveis, mensagem);
    }

    private SituacaoBlocoEnum situacao(EspacoFisico espaco, BlocoHorario bloco, Set<Integer> ocupados) {
        if (bloco.getTipo() == TipoBlocoEnum.TREINO) {
            return SituacaoBlocoEnum.TREINO;
        }
        if (bloco.getTipo() == TipoBlocoEnum.MANUTENCAO || !espaco.isReservavel()) {
            return SituacaoBlocoEnum.MANUTENCAO;
        }
        return ocupados.contains(bloco.getId()) ? SituacaoBlocoEnum.OCUPADO : SituacaoBlocoEnum.LIVRE;
    }

    private EspacoFisico obterEspaco(Integer id) {
        return espacoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Espaço não encontrado."));
    }
}
