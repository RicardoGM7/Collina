package br.ufs.dcomp.collina.service.financeiro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ufs.dcomp.collina.dto.financeiro.GastoAvulsoRequestDTO;
import br.ufs.dcomp.collina.dto.financeiro.MensalidadeResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.associados.Associado;
import br.ufs.dcomp.collina.model.associados.Pessoa;
import br.ufs.dcomp.collina.model.associados.SituacaoCadastralEnum;
import br.ufs.dcomp.collina.model.associados.StatusVinculoEnum;
import br.ufs.dcomp.collina.model.associados.VinculoDependencia;
import br.ufs.dcomp.collina.model.financeiro.GastoAvulso;
import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.repository.associados.AssociadoRepository;
import br.ufs.dcomp.collina.repository.associados.PessoaRepository;
import br.ufs.dcomp.collina.repository.associados.VinculoDependenciaRepository;
import br.ufs.dcomp.collina.repository.financeiro.MensalidadeRepository;
import br.ufs.dcomp.collina.service.financeiro.integracao.VerificadorDeFuncionario;

@Service
public class MensalidadeService implements AberturaMensalidadePort {

    @Autowired
    private MensalidadeRepository mensalidadeRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private AssociadoRepository associadoRepository;

    @Autowired
    private VinculoDependenciaRepository vinculoDependenciaRepository;

    @Autowired
    private VerificadorDeFuncionario verificadorDeFuncionario;

    @Override
    @Transactional
    public void abrirMensalidadeInicial(Associado titular, YearMonth competencia) {
        if (titular == null) {
            throw new IllegalArgumentException("O associado titular não pode ser nulo.");
        }
        if (titular.getId() == null) {
            throw new IllegalArgumentException("O associado titular deve possuir ID.");
        }
        if (competencia == null) {
            throw new IllegalArgumentException("A competência não pode ser nula.");
        }
        if (mensalidadeRepository.buscarEmAbertoDoTitular(titular.getId()).isPresent()) {
            return;
        }

        Mensalidade mensalidade = new Mensalidade(
                competencia.toString(),
                titular.getPlano().getMensalidade(),
                titular
        );
        mensalidadeRepository.save(mensalidade);
    }

    // Uma única transação: a consulta já garante as precondições da entidade
    // (PENDENTE e vencimento < hoje), então uma falha aqui indicaria bug.
    // Sem @Transactional as alterações nunca seriam gravadas.
    @Transactional
    public void marcarAtrasadas(LocalDate hoje) {
        if (hoje == null) {
            throw new IllegalArgumentException("A data de referência não pode ser nula.");
        }

        mensalidadeRepository.buscarMensalidadesPendentesVencidas(hoje)
                .forEach(mensalidade -> mensalidade.marcarComoAtrasada(hoje));
    }

    @Transactional(readOnly = true)
    public List<MensalidadeResponseDTO> listarMensalidades(Integer associadoId, YearMonth competencia) {
        if (associadoId == null) {
            throw new IllegalArgumentException("O id do associado não pode ser nulo.");
        }
        if (competencia == null) {
            throw new IllegalArgumentException("A competência não pode ser nula.");
        }

        return mensalidadeRepository
                .findByAssociadoTitular_IdAndCompetencia(associadoId, competencia.toString())
                .stream()
                .map(MensalidadeResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MensalidadeResponseDTO> listarMensalidadesDoAssociado(Integer associadoId, YearMonth competencia) {
        if (associadoId == null) {
            throw new IllegalArgumentException("O id do associado não pode ser nulo.");
        }

        if (competencia != null) {
            return listarMensalidades(associadoId, competencia);
        }

        return mensalidadeRepository
                .findByAssociadoTitular_IdOrderByCompetenciaDesc(associadoId)
                .stream()
                .map(MensalidadeResponseDTO::new)
                .toList();
    }

    @Transactional
    public MensalidadeResponseDTO incluirGastoAvulso(GastoAvulsoRequestDTO dto, Long funcionarioId, LocalDateTime dataLancamento) {
        if (dto == null) {
            throw new IllegalArgumentException("Informe os dados do gasto avulso.");
        }
        if (funcionarioId == null) {
            throw new IllegalArgumentException("O funcionário responsável pelo lançamento é obrigatório.");
        }
        if (dataLancamento == null) {
            throw new IllegalArgumentException("A data de lançamento não pode ser nula.");
        }
        verificadorDeFuncionario.exigirFuncionarioHabilitado(funcionarioId);

        // 1. Resolver a Pessoa
        Pessoa pessoa = pessoaRepository.findById(dto.pessoaId())
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada com o ID: " + dto.pessoaId()));

        Associado titular;
        VinculoDependencia vinculoDependencia = null;

        // Se a pessoa for o titular direto
        var titularOpt = associadoRepository.findByPessoaCpf(pessoa.getCpf());
        if (titularOpt.isPresent()) {
            titular = titularOpt.get();
        } else {
            // Se for dependente, busca vínculo ativo
            VinculoDependencia vinculo = vinculoDependenciaRepository
                    .findByDependenteIdAndStatus(pessoa.getId(), StatusVinculoEnum.ATIVO)
                    .orElseThrow(() -> new IllegalStateException(
                            "A pessoa não é titular e não possui vínculo ativo de dependência com nenhum titular."));
            titular = vinculo.getTitular();
            vinculoDependencia = vinculo;
        }

        // 2. Titular inativo não pode ter lançamentos
        if (titular.getSituacaoCadastral() == SituacaoCadastralEnum.INATIVO) {
            throw new IllegalStateException("Não é permitido lançar gasto avulso para associado titular inativo.");
        }

        // 3. Buscar mensalidade EM_ABERTO do titular
        Mensalidade mensalidade = mensalidadeRepository.buscarEmAbertoDoTitular(titular.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Não há mensalidade em aberto para o titular " + titular.getId() + "."));

        // 4. Criar GastoAvulso e vincular
        GastoAvulso gasto = new GastoAvulso(
                dto.valor(),
                dto.descricao(),
                dataLancamento,
                funcionarioId,
                vinculoDependencia
        );

        mensalidade.adicionarGastoAvulso(gasto);

        return new MensalidadeResponseDTO(mensalidade);
    }
}
