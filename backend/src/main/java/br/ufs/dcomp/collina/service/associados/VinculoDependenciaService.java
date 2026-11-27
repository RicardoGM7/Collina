package br.ufs.dcomp.collina.service.associados;

import br.ufs.dcomp.collina.dto.associados.DependenteRequestDTO;
import br.ufs.dcomp.collina.dto.associados.DependenteUpdateDTO;
import br.ufs.dcomp.collina.dto.associados.VinculoDependenciaResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.associados.Associado;
import br.ufs.dcomp.collina.model.associados.MotivoInativacaoVinculoEnum;
import br.ufs.dcomp.collina.model.auth.Pessoa;
import br.ufs.dcomp.collina.model.associados.SituacaoCadastralEnum;
import br.ufs.dcomp.collina.model.associados.StatusVinculoEnum;
import br.ufs.dcomp.collina.model.associados.VinculoDependencia;
import br.ufs.dcomp.collina.repository.associados.AssociadoRepository;
import br.ufs.dcomp.collina.repository.auth.PessoaRepository;
import br.ufs.dcomp.collina.repository.associados.VinculoDependenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class VinculoDependenciaService {

    // RF-AS04: ao completar 21 anos a pessoa perde a condição de dependente
    public static final int IDADE_LIMITE_DEPENDENTE = 21;

    @Autowired
    private VinculoDependenciaRepository vinculoRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private AssociadoRepository associadoRepository;

    @Autowired(required = false)
    private Clock clock;

    // 1. RF-AS03: cadastrar dependente vinculado a um titular (feito por funcionário da recepção)
    @Transactional
    public VinculoDependenciaResponseDTO cadastrarDependente(Integer titularId, DependenteRequestDTO dto) {
        if (titularId == null) {
            throw new IllegalArgumentException("O ID do titular é obrigatório.");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Os dados do dependente não foram informados.");
        }
        if (dto.nome() == null || dto.nome().trim().isBlank()) {
            throw new IllegalArgumentException("O nome do dependente é obrigatório.");
        }
        if (dto.dataNascimento() == null) {
            throw new IllegalArgumentException("A data de nascimento do dependente é obrigatória.");
        }
        String cpf = CpfValidador.validar(dto.cpf());

        Associado titular = obterTitular(titularId);
        if (titular.getSituacaoCadastral() == SituacaoCadastralEnum.INATIVO) {
            throw new IllegalStateException("Não é possível cadastrar dependentes para um titular inativo.");
        }

        LocalDate hoje = LocalDate.now();

        // RF-AS01: o CPF identifica unicamente a pessoa; reaproveita o registro se ela já existir
        Pessoa pessoa = pessoaRepository.findByCpf(cpf).orElse(null);
        if (pessoa == null) {
            pessoa = new Pessoa();
            pessoa.setCpf(cpf);
            pessoa.setNome(dto.nome().trim());
            pessoa.setDataNascimento(dto.dataNascimento());
        } else {
            if (associadoRepository.existsByPessoaCpf(cpf)) {
                throw new IllegalStateException("Esta pessoa já é associada titular e não pode ser cadastrada como dependente.");
            }
            if (pessoa.getId() != null
                    && vinculoRepository.findByDependenteIdAndStatus(pessoa.getId(), StatusVinculoEnum.ATIVO).isPresent()) {
                throw new IllegalStateException("Esta pessoa já possui um vínculo de dependência ativo.");
            }
        }

        // RF-AS04: elegibilidade pela idade
        if (pessoa.idadeEm(hoje) >= IDADE_LIMITE_DEPENDENTE) {
            throw new IllegalArgumentException("Pessoas com " + IDADE_LIMITE_DEPENDENTE
                    + " anos ou mais não podem ser cadastradas como dependentes.");
        }

        // RF-AS02/AS03: respeitar a quantidade máxima de dependentes ativos do plano do titular
        long ativos = vinculoRepository.countByTitularIdAndStatus(titularId, StatusVinculoEnum.ATIVO);
        if (ativos >= titular.getPlano().getMaxDependentesAtivos()) {
            throw new IllegalStateException("O titular atingiu o limite de dependentes ativos do plano ("
                    + titular.getPlano().getMaxDependentesAtivos() + ").");
        }

        Pessoa pessoaSalva = pessoaRepository.save(pessoa);

        VinculoDependencia vinculo = new VinculoDependencia();
        vinculo.setTitular(titular);
        vinculo.setDependente(pessoaSalva);
        vinculo.setDataInicio(hoje);
        vinculo.setStatus(StatusVinculoEnum.ATIVO);

        return new VinculoDependenciaResponseDTO(vinculoRepository.save(vinculo));
    }

    // 2. Listar vínculos de um titular (opcionalmente filtrando por status)
    @Transactional(readOnly = true)
    public List<VinculoDependenciaResponseDTO> listarPorTitular(Integer titularId, StatusVinculoEnum status) {
        if (titularId == null) {
            throw new IllegalArgumentException("O ID do titular é obrigatório.");
        }
        obterTitular(titularId);

        return vinculoRepository.findByTitularId(titularId)
                .stream()
                .filter(v -> status == null || v.getStatus() == status)
                .map(VinculoDependenciaResponseDTO::new)
                .toList();
    }

    // 3. Buscar vínculo por ID
    @Transactional(readOnly = true)
    public VinculoDependenciaResponseDTO buscarPorId(Integer vinculoId) {
        if (vinculoId == null) {
            throw new IllegalArgumentException("O ID do vínculo é obrigatório.");
        }
        return new VinculoDependenciaResponseDTO(obterVinculo(vinculoId));
    }

    // 4. RF-AS03: atualizar dados pessoais do dependente (funcionário autorizado)
    @Transactional
    public VinculoDependenciaResponseDTO atualizarDependente(Integer vinculoId, DependenteUpdateDTO dto) {
        if (vinculoId == null) {
            throw new IllegalArgumentException("O ID do vínculo é obrigatório.");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Os dados do dependente não foram informados.");
        }
        if (dto.nome() == null || dto.nome().trim().isBlank()) {
            throw new IllegalArgumentException("O nome do dependente não pode ser vazio.");
        }
        if (dto.dataNascimento() == null) {
            throw new IllegalArgumentException("A data de nascimento do dependente é obrigatória.");
        }

        VinculoDependencia vinculo = obterVinculo(vinculoId);
        Pessoa pessoa = vinculo.getDependente();

        // Um vínculo ativo não pode passar a ter dependente com 21 anos ou mais
        if (vinculo.getStatus() == StatusVinculoEnum.ATIVO
                && java.time.Period.between(dto.dataNascimento(), LocalDate.now(obterClock())).getYears() >= IDADE_LIMITE_DEPENDENTE) {
            throw new IllegalArgumentException("A nova data de nascimento tornaria o dependente inelegível (limite de "
                    + IDADE_LIMITE_DEPENDENTE + " anos).");
        }

        pessoa.setNome(dto.nome().trim());
        pessoa.setDataNascimento(dto.dataNascimento());
        pessoaRepository.save(pessoa);

        return new VinculoDependenciaResponseDTO(vinculo);
    }

    // 5. RF-AS03: inativar o vínculo por solicitação (o registro nunca é apagado)
    @Transactional
    public VinculoDependenciaResponseDTO inativarVinculo(Integer vinculoId) {
        if (vinculoId == null) {
            throw new IllegalArgumentException("O ID do vínculo é obrigatório.");
        }
        VinculoDependencia vinculo = obterVinculo(vinculoId);
        if (vinculo.getStatus() == StatusVinculoEnum.INATIVO) {
            throw new IllegalStateException("O vínculo de dependência já está inativo.");
        }
        inativar(vinculo, MotivoInativacaoVinculoEnum.SOLICITACAO, LocalDate.now());
        return new VinculoDependenciaResponseDTO(vinculo);
    }

    // 6. RF-AS04: inativa os vínculos ativos cujo dependente já completou 21 anos.
    // Disparo manual (endpoint POST /api/dependentes/inativar-por-idade). Retorna quantos vínculos foram inativados.
    @Transactional
    public int inativarVinculosVencidosPorIdade() {
        LocalDate hoje = LocalDate.now(obterClock());
        int total = 0;
        for (VinculoDependencia vinculo : vinculoRepository.findByStatus(StatusVinculoEnum.ATIVO)) {
            if (vinculo.getDependente().idadeEm(hoje) >= IDADE_LIMITE_DEPENDENTE) {
                inativar(vinculo, MotivoInativacaoVinculoEnum.IDADE, hoje);
                total++;
            }
        }
        return total;
    }

    // 6.1 Usado pelo InativacaoDependenteAgendador: lista os IDs elegíveis para inativação por idade
    @Transactional(readOnly = true)
    public List<Integer> listarIdsVinculosVencidosPorIdade(LocalDate dataReferencia) {
        LocalDate referencia = dataReferencia != null ? dataReferencia : LocalDate.now(obterClock());
        return vinculoRepository.findByStatus(StatusVinculoEnum.ATIVO).stream()
                .filter(v -> v.getDependente().idadeEm(referencia) >= IDADE_LIMITE_DEPENDENTE)
                .map(VinculoDependencia::getId)
                .toList();
    }

    // 6.2 Usado pelo InativacaoDependenteAgendador: inativa um único vínculo em transação independente (idempotente)
    @Transactional
    public boolean inativarVinculoPorIdadeSeVencido(Integer vinculoId, LocalDate dataReferencia) {
        if (vinculoId == null) {
            return false;
        }
        LocalDate referencia = dataReferencia != null ? dataReferencia : LocalDate.now(obterClock());
        VinculoDependencia vinculo = vinculoRepository.findById(vinculoId).orElse(null);
        if (vinculo == null || vinculo.getStatus() != StatusVinculoEnum.ATIVO) {
            return false;
        }
        if (vinculo.getDependente().idadeEm(referencia) < IDADE_LIMITE_DEPENDENTE) {
            return false;
        }
        inativar(vinculo, MotivoInativacaoVinculoEnum.IDADE, referencia);
        return true;
    }

    private Clock obterClock() {
        return clock != null ? clock : Clock.systemDefaultZone();
    }

    // 7. RF-AS04/AS08: elegibilidade do dependente como beneficiário de reserva na data de utilização.
    // Usado pelo módulo de Reservas (Gestão de Espaços). Recebe o id da Pessoa do dependente.
    @Transactional(readOnly = true)
    public boolean isDependenteElegivel(Integer dependenteId, LocalDate dataUtilizacao) {
        if (dependenteId == null || dataUtilizacao == null) {
            throw new IllegalArgumentException("O dependente e a data de utilização são obrigatórios.");
        }
        return vinculoRepository.findByDependenteIdAndStatus(dependenteId, StatusVinculoEnum.ATIVO)
                .filter(v -> v.getTitular().getSituacaoCadastral() == SituacaoCadastralEnum.ATIVO)
                .filter(v -> v.getDependente().idadeEm(dataUtilizacao) < IDADE_LIMITE_DEPENDENTE)
                .isPresent();
    }

    // ---- Métodos usados pelo AssociadoService (RF-AS04 e RF-AS06) ----

    // RF-AS04: inativa o vínculo de uma pessoa que passou a ter 21 anos antes de a rotina rodar
    @Transactional
    public void inativarPorIdade(VinculoDependencia vinculo) {
        inativar(vinculo, MotivoInativacaoVinculoEnum.IDADE, LocalDate.now());
    }

    // RF-AS06: a inativação do titular inativa automaticamente todos os seus vínculos ativos
    @Transactional
    public void inativarVinculosPorInativacaoDoTitular(Associado titular) {
        LocalDate hoje = LocalDate.now();
        for (VinculoDependencia vinculo : vinculoRepository.findByTitularId(titular.getId())) {
            if (vinculo.getStatus() == StatusVinculoEnum.ATIVO) {
                inativar(vinculo, MotivoInativacaoVinculoEnum.INATIVACAO_TITULAR, hoje);
            }
        }
    }

    // RF-AS06: ao reativar o titular, voltam apenas os vínculos inativados por INATIVACAO_TITULAR,
    // desde que a pessoa ainda tenha menos de 21 anos, o limite do plano não seja ultrapassado
    // e ela não tenha virado titular ou dependente ativo de outro titular nesse meio tempo.
    @Transactional
    public void reativarVinculosDoTitular(Associado titular) {
        LocalDate hoje = LocalDate.now();
        long ativos = vinculoRepository.countByTitularIdAndStatus(titular.getId(), StatusVinculoEnum.ATIVO);
        int maximo = titular.getPlano().getMaxDependentesAtivos();

        List<VinculoDependencia> candidatos = vinculoRepository
                .findByTitularIdAndStatusAndMotivoInativacao(
                        titular.getId(), StatusVinculoEnum.INATIVO, MotivoInativacaoVinculoEnum.INATIVACAO_TITULAR)
                .stream()
                .sorted(Comparator.comparing(VinculoDependencia::getDataInicio))
                .toList();

        for (VinculoDependencia vinculo : candidatos) {
            Pessoa dependente = vinculo.getDependente();
            if (ativos >= maximo) {
                break;
            }
            if (dependente.idadeEm(hoje) >= IDADE_LIMITE_DEPENDENTE) {
                continue;
            }
            if (associadoRepository.existsByPessoaCpf(dependente.getCpf())) {
                continue;
            }
            if (vinculoRepository.findByDependenteIdAndStatus(dependente.getId(), StatusVinculoEnum.ATIVO).isPresent()) {
                continue;
            }
            vinculo.setStatus(StatusVinculoEnum.ATIVO);
            vinculo.setDataFim(null);
            vinculo.setMotivoInativacao(null);
            vinculoRepository.save(vinculo);
            ativos++;
        }
    }

    // ---- Auxiliares ----

    private void inativar(VinculoDependencia vinculo, MotivoInativacaoVinculoEnum motivo, LocalDate data) {
        vinculo.setStatus(StatusVinculoEnum.INATIVO);
        vinculo.setDataFim(data);
        vinculo.setMotivoInativacao(motivo);
        vinculoRepository.save(vinculo);
    }

    private Associado obterTitular(Integer titularId) {
        return associadoRepository.findById(titularId)
                .orElseThrow(() -> new ResourceNotFoundException("Associado não encontrado com o ID: " + titularId));
    }

    private VinculoDependencia obterVinculo(Integer vinculoId) {
        return vinculoRepository.findById(vinculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo de dependência não encontrado com o ID: " + vinculoId));
    }
}
