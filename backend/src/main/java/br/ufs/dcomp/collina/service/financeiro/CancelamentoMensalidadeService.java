package br.ufs.dcomp.collina.service.financeiro;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ufs.dcomp.collina.dto.financeiro.CancelamentoDeMensalidadeRequestDTO;
import br.ufs.dcomp.collina.dto.financeiro.MensalidadeResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.financeiro.CancelamentoDeMensalidade;
import br.ufs.dcomp.collina.model.financeiro.GastoAvulso;
import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.repository.financeiro.CancelamentoDeMensalidadeRepository;
import br.ufs.dcomp.collina.repository.financeiro.MensalidadeRepository;
import br.ufs.dcomp.collina.service.financeiro.integracao.VerificadorDeFuncionario;

@Service 
public class CancelamentoMensalidadeService {
    
    @Autowired 
    private MensalidadeRepository mensalidadeRepository;

    @Autowired
    private CancelamentoDeMensalidadeRepository cancelamentoDeMensalidadeRepository;

    @Autowired
    private VerificadorDeFuncionario verificadorDeFuncionario;

    @Transactional
    public MensalidadeResponseDTO cancelarEReemitir(CancelamentoDeMensalidadeRequestDTO dto, Long mensalidadeId, Long funcionarioId){
        if (funcionarioId == null) {
            throw new IllegalArgumentException("O funcionário responsável pelo cancelamento é obrigatório.");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Dados de cancelamento não informados.");
        }
        if (dto.novoVencimento() == null) {
            throw new IllegalArgumentException("A data de vencimento é obrigatória.");
        }
        if (dto.novoVencimento().getDayOfWeek() == DayOfWeek.SATURDAY || dto.novoVencimento().getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("A data de vencimento deve ser um dia útil (segunda a sexta-feira).");
        }

        verificadorDeFuncionario.exigirFuncionarioHabilitado(funcionarioId);

        // Bloqueio pessimista para evitar concorrência (RF-F11)
        Mensalidade original = mensalidadeRepository.buscarPorIdParaAtualizar(mensalidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensalidade não encontrada com o ID: " + mensalidadeId));

        original.cancelar();
        Mensalidade substituta = original.reemitir(dto.novoValor(), dto.novoVencimento());

        mensalidadeRepository.save(substituta);

        CancelamentoDeMensalidade cancelamentoDeMensalidade = new CancelamentoDeMensalidade(
                dto.justificativa(),
                LocalDateTime.now(),
                original,
                substituta,
                funcionarioId
        );
        cancelamentoDeMensalidadeRepository.save(cancelamentoDeMensalidade);

        return new MensalidadeResponseDTO(substituta);
    }
}
