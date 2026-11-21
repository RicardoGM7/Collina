package br.ufs.dcomp.collina.service.associados;

import br.ufs.dcomp.collina.dto.associados.AssociadoRequestDTO;
import br.ufs.dcomp.collina.dto.associados.AssociadoResponseDTO;
import br.ufs.dcomp.collina.dto.associados.AssociadoUpdateDTO;
import br.ufs.dcomp.collina.dto.auth.UsuarioCriarRequestDTO;
import br.ufs.dcomp.collina.dto.auth.UsuarioResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.associados.Associado;
import br.ufs.dcomp.collina.model.associados.Pessoa;
import br.ufs.dcomp.collina.model.associados.Plano;
import br.ufs.dcomp.collina.model.associados.SituacaoCadastralEnum;
import br.ufs.dcomp.collina.model.associados.StatusVinculoEnum;
import br.ufs.dcomp.collina.model.associados.TipoPlanoEnum;
import br.ufs.dcomp.collina.model.auth.PerfilUsuarioEnum;
import br.ufs.dcomp.collina.repository.associados.AssociadoRepository;
import br.ufs.dcomp.collina.repository.associados.PessoaRepository;
import br.ufs.dcomp.collina.repository.associados.PlanoRepository;
import br.ufs.dcomp.collina.repository.associados.VinculoDependenciaRepository;
import br.ufs.dcomp.collina.service.auth.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AssociadoService {

    @Autowired
    private AssociadoRepository associadoRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private PlanoRepository planoRepository;

    @Autowired
    private VinculoDependenciaRepository vinculoRepository;

    @Autowired
    private VinculoDependenciaService vinculoService;

    @Autowired(required = false)
    private UsuarioService usuarioService;

    // 1. RF-AS01 + RF-AS02: cadastrar associado titular já vinculado a um plano
    @Transactional
    public AssociadoResponseDTO cadastrar(AssociadoRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Os dados do associado não foram informados.");
        }
        if (dto.nome() == null || dto.nome().trim().isBlank()) {
            throw new IllegalArgumentException("O nome do associado é obrigatório.");
        }
        if (dto.dataNascimento() == null) {
            throw new IllegalArgumentException("A data de nascimento é obrigatória.");
        }
        if (dto.categoria() == null) {
            throw new IllegalArgumentException("A categoria do associado é obrigatória.");
        }
        if (dto.tipoPlano() == null) {
            throw new IllegalArgumentException("O plano do associado é obrigatório.");
        }
        String cpf = CpfValidador.validar(dto.cpf());

        // RF-AS02: Contribuinte -> Básico/Plus/Premium; Proprietário -> Proprietário
        if (dto.tipoPlano().getCategoriaPermitida() != dto.categoria()) {
            throw new IllegalArgumentException("O plano " + dto.tipoPlano() + " não é permitido para a categoria "
                    + dto.categoria() + ".");
        }
        Plano plano = planoRepository.findByTipo(dto.tipoPlano())
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + dto.tipoPlano()));

        if (associadoRepository.existsByPessoaCpf(cpf)) {
            throw new IllegalStateException("Já existe um associado titular cadastrado com este CPF.");
        }
        if (dto.usuarioId() != null && !dto.usuarioId().isBlank()
                && associadoRepository.existsByUsuarioId(dto.usuarioId().trim())) {
            throw new IllegalStateException("Esta conta de acesso já está vinculada a outro associado titular.");
        }

        LocalDate hoje = LocalDate.now();
        Pessoa pessoa = pessoaRepository.findByCpf(cpf).orElse(null);

        if (pessoa == null) {
            pessoa = new Pessoa();
            pessoa.setCpf(cpf);
            pessoa.setNome(dto.nome().trim());
            pessoa.setDataNascimento(dto.dataNascimento());
            pessoa = pessoaRepository.save(pessoa);
        } else {
            // RF-AS01/AS04: a pessoa já existe (ex.: ex-dependente que solicitou admissão).
            // Os dados pessoais já registrados são reaproveitados e o histórico do vínculo é preservado.
            Pessoa existente = pessoa;
            vinculoRepository.findByDependenteIdAndStatus(existente.getId(), StatusVinculoEnum.ATIVO)
                    .ifPresent(vinculo -> {
                        if (existente.idadeEm(hoje) >= VinculoDependenciaService.IDADE_LIMITE_DEPENDENTE) {
                            // já deveria ter perdido a condição de dependente, só a rotina ainda não rodou
                            vinculoService.inativarPorIdade(vinculo);
                        } else {
                            throw new IllegalStateException(
                                    "Esta pessoa está registrada como dependente ativo e não pode ser cadastrada como titular.");
                        }
                    });
        }

        String usuarioId = dto.usuarioId() != null && !dto.usuarioId().isBlank()
                ? dto.usuarioId().trim()
                : null;

        boolean temEmail = dto.email() != null && !dto.email().isBlank();
        boolean temSenha = dto.senha() != null && !dto.senha().isBlank();
        if (usuarioId == null && (temEmail || temSenha)) {
            if (!temEmail || !temSenha) {
                throw new IllegalArgumentException("Informe e-mail e senha para criar a conta de acesso do associado.");
            }
            if (usuarioService == null) {
                throw new IllegalStateException("Serviço de autenticação indisponível para criar a conta de acesso.");
            }
            UsuarioResponseDTO conta = usuarioService.criar(new UsuarioCriarRequestDTO(
                    pessoa.getNome(),
                    dto.email().trim(),
                    dto.senha(),
                    PerfilUsuarioEnum.ASSOCIADO
            ));
            usuarioId = conta.uid();
        }

        Associado associado = new Associado();
        associado.setPessoa(pessoa);
        associado.setCategoria(dto.categoria());
        associado.setPlano(plano);
        associado.setSituacaoCadastral(SituacaoCadastralEnum.ATIVO);
        associado.setDataAdmissao(hoje);
        associado.setUsuarioId(usuarioId);

        return new AssociadoResponseDTO(associadoRepository.save(associado));
    }

    // 2. Listar associados (opcionalmente por situação cadastral)
    @Transactional(readOnly = true)
    public List<AssociadoResponseDTO> listar(SituacaoCadastralEnum situacao) {
        List<Associado> associados = situacao == null
                ? associadoRepository.findAll()
                : associadoRepository.findBySituacaoCadastralIn(List.of(situacao));
        return associados.stream().map(AssociadoResponseDTO::new).toList();
    }

    // 3. Buscar associado por ID
    @Transactional(readOnly = true)
    public AssociadoResponseDTO buscarPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do associado é obrigatório.");
        }
        return new AssociadoResponseDTO(obterAssociado(id));
    }

    // 4. Buscar associado por CPF
    @Transactional(readOnly = true)
    public AssociadoResponseDTO buscarPorCpf(String cpf) {
        String cpfValido = CpfValidador.validar(cpf);
        Associado associado = associadoRepository.findByPessoaCpf(cpfValido)
                .orElseThrow(() -> new ResourceNotFoundException("Associado não encontrado com o CPF informado."));
        return new AssociadoResponseDTO(associado);
    }

    // 4.1 Buscar associado pelo usuarioId autenticado (RF-AS01 / RF-AUTH01)
    @Transactional(readOnly = true)
    public AssociadoResponseDTO buscarPorUsuarioId(String usuarioId) {
        if (usuarioId == null || usuarioId.trim().isBlank()) {
            throw new IllegalArgumentException("O ID do usuário é obrigatório.");
        }
        return new AssociadoResponseDTO(obterPorUsuarioId(usuarioId));
    }

    // 4.2 RF-AS05: o titular autenticado altera os próprios dados (identificado pelo token, não por ID do cliente)
    @Transactional
    public AssociadoResponseDTO atualizarMeusDados(String usuarioId, AssociadoUpdateDTO dto) {
        return atualizarDadosPessoais(obterPorUsuarioId(usuarioId).getId(), dto);
    }

    // Id do associado vinculado ao usuário autenticado (usado para consultas do próprio titular)
    @Transactional(readOnly = true)
    public Integer obterIdPorUsuarioId(String usuarioId) {
        return obterPorUsuarioId(usuarioId).getId();
    }

    private Associado obterPorUsuarioId(String usuarioId) {
        if (usuarioId == null || usuarioId.trim().isBlank()) {
            throw new IllegalArgumentException("O ID do usuário é obrigatório.");
        }
        return associadoRepository.findByUsuarioId(usuarioId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum cadastro de associado vinculado ao usuário autenticado."));
    }

    // 5. RF-AS05: o titular altera seus próprios dados pessoais (CPF, categoria, plano e situação não mudam aqui)
    @Transactional
    public AssociadoResponseDTO atualizarDadosPessoais(Integer id, AssociadoUpdateDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do associado é obrigatório.");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Os dados para atualização são obrigatórios.");
        }
        if (dto.nome() == null || dto.nome().trim().isBlank()) {
            throw new IllegalArgumentException("O nome não pode ser vazio.");
        }
        if (dto.dataNascimento() == null || !dto.dataNascimento().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data de nascimento deve estar no passado.");
        }

        Associado associado = obterAssociado(id);
        Pessoa pessoa = associado.getPessoa();
        pessoa.setNome(dto.nome().trim());
        pessoa.setDataNascimento(dto.dataNascimento());
        pessoaRepository.save(pessoa);

        return new AssociadoResponseDTO(associado);
    }

    // 6. RF-AS02/AS05: alteração de plano por funcionário autorizado.
    // A categoria acompanha o plano (Proprietário <-> Contribuinte). O novo plano precisa comportar
    // a quantidade de dependentes ativos que o titular já possui.
    @Transactional
    public AssociadoResponseDTO alterarPlano(Integer id, TipoPlanoEnum novoTipo) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do associado é obrigatório.");
        }
        if (novoTipo == null) {
            throw new IllegalArgumentException("O novo plano é obrigatório.");
        }

        Associado associado = obterAssociado(id);
        Plano novoPlano = planoRepository.findByTipo(novoTipo)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + novoTipo));

        if (associado.getPlano().getId().equals(novoPlano.getId())) {
            throw new IllegalStateException("O associado já está vinculado ao plano " + novoTipo + ".");
        }

        long dependentesAtivos = vinculoRepository.countByTitularIdAndStatus(id, StatusVinculoEnum.ATIVO);
        if (dependentesAtivos > novoPlano.getMaxDependentesAtivos()) {
            throw new IllegalStateException("O plano " + novoTipo + " permite no máximo "
                    + novoPlano.getMaxDependentesAtivos() + " dependentes ativos, mas o titular possui "
                    + dependentesAtivos + ". Inative dependentes antes de trocar de plano.");
        }

        associado.setPlano(novoPlano);
        associado.setCategoria(novoTipo.getCategoriaPermitida());

        return new AssociadoResponseDTO(associadoRepository.save(associado));
    }

    // 7. RF-AS06: controle da situação cadastral (funcionário autorizado)
    //  - SUSPENSO: não altera os vínculos (o acesso dos dependentes é bloqueado na verificação de elegibilidade)
    //  - INATIVO: inativa automaticamente os vínculos ativos (motivo INATIVACAO_TITULAR)
    //  - INATIVO -> ATIVO: reativa só os vínculos inativados por INATIVACAO_TITULAR
    @Transactional
    public AssociadoResponseDTO alterarSituacao(Integer id, SituacaoCadastralEnum novaSituacao) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do associado é obrigatório.");
        }
        if (novaSituacao == null) {
            throw new IllegalArgumentException("A nova situação cadastral é obrigatória.");
        }

        Associado associado = obterAssociado(id);
        SituacaoCadastralEnum atual = associado.getSituacaoCadastral();

        if (atual == novaSituacao) {
            throw new IllegalStateException("O associado já está na situação " + novaSituacao + ".");
        }
        if (atual == SituacaoCadastralEnum.INATIVO && novaSituacao == SituacaoCadastralEnum.SUSPENSO) {
            throw new IllegalStateException("Um associado inativo deve ser reativado (ATIVO) antes de ser suspenso.");
        }

        associado.setSituacaoCadastral(novaSituacao);
        Associado salvo = associadoRepository.save(associado);

        if (novaSituacao == SituacaoCadastralEnum.INATIVO) {
            vinculoService.inativarVinculosPorInativacaoDoTitular(salvo);
        } else if (atual == SituacaoCadastralEnum.INATIVO && novaSituacao == SituacaoCadastralEnum.ATIVO) {
            vinculoService.reativarVinculosDoTitular(salvo);
        }

        return new AssociadoResponseDTO(salvo);
    }

    private Associado obterAssociado(Integer id) {
        return associadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Associado não encontrado com o ID: " + id));
    }
}
