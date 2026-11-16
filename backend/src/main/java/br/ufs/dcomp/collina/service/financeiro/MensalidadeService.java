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
import br.ufs.dcomp.collina.model.financeiro.GastoAvulso;
import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.repository.financeiro.MensalidadeRepository;
import br.ufs.dcomp.collina.service.financeiro.integracao.ConsultaTitularResponsavel;
import br.ufs.dcomp.collina.service.financeiro.integracao.VerificadorDeFuncionario;

@Service
public class MensalidadeService {

    @Autowired
    private MensalidadeRepository mensalidadeRepository;

    @Autowired
    private ConsultaTitularResponsavel consultaTitular;

    @Autowired
    private VerificadorDeFuncionario verificadorDeFuncionario;

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
    public List<MensalidadeResponseDTO> listarMensalidades(Long associadoId, YearMonth competencia) {
        if (associadoId == null) {
            throw new IllegalArgumentException("O id do associado não pode ser nulo.");
        }
        if (competencia == null) {
            throw new IllegalArgumentException("A competência não pode ser nula.");
        }

        return mensalidadeRepository
                .findByAssociadoTitularAndCompetencia(associadoId, competencia.toString())
                .stream()
                .map(MensalidadeResponseDTO::new)
                .toList();
    }

    @Transactional
    public MensalidadeResponseDTO incluirGastoAvulso(GastoAvulsoRequestDTO dto, Long funcionarioId) {
        if (dto == null) {
            throw new IllegalArgumentException("Informe os dados do gasto avulso.");
        }
        verificadorDeFuncionario.exigirFuncionarioHabilitado(funcionarioId);

        // Titular ou dependente: o gasto sempre vai para a mensalidade do titular.
        Long titularId = consultaTitular.buscarIdTitularResponsavel(dto.autorDoGastoId());

        Mensalidade mensalidade = mensalidadeRepository.buscarEmAbertoDoTitular(titularId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Não há mensalidade em aberto para o titular " + titularId + "."));

        GastoAvulso gasto = new GastoAvulso();
        gasto.setValor(dto.valor());
        gasto.setDescricao(dto.descricao());
        gasto.setDataLancamento(LocalDateTime.now());
        gasto.setFuncionario(funcionarioId);

        // A entidade valida estado e valor e só então altera. Sem save explícito:
        // a mensalidade está gerenciada e o cascade ALL grava o gasto no commit.
        mensalidade.adicionarGastoAvulso(gasto);

        return new MensalidadeResponseDTO(mensalidade);
    }
}
