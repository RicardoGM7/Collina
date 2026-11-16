package br.ufs.dcomp.collina.service.financeiro;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ufs.dcomp.collina.dto.financeiro.CancelamentoDeMensalidadeRequestDTO;
import br.ufs.dcomp.collina.dto.financeiro.MensalidadeResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.financeiro.CancelamentoDeMensalidade;
import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.repository.financeiro.CancelamentoDeMensalidadeRepository;
import br.ufs.dcomp.collina.repository.financeiro.MensalidadeRepository;

@Service 
public class CancelamentoMensalidadeService {
    
    @Autowired 
    private MensalidadeRepository mensalidadeRepository;

    @Autowired
    private CancelamentoDeMensalidadeRepository cancelamentoDeMensalidadeRepository;

    @Transactional
    public MensalidadeResponseDTO cancelarEReemitir(CancelamentoDeMensalidadeRequestDTO dto, Long mensalidadeId){
        Mensalidade original = mensalidadeRepository.findById(mensalidadeId).orElseThrow(() -> new ResourceNotFoundException("Mensalidade não encontrada"));
        /*ainda tem que fazer todo o trampo do funcionario dps */
        original.cancelar();
        Mensalidade substituta = original.reemitir(dto.novoValor(), dto.novoVencimento());

        mensalidadeRepository.save(substituta);

        CancelamentoDeMensalidade cancelamentoDeMensalidade = new CancelamentoDeMensalidade(dto.justificativa(), LocalDateTime.now(), original, substituta, dto.funcionarioId());
        cancelamentoDeMensalidadeRepository.save(cancelamentoDeMensalidade);

        return new MensalidadeResponseDTO(substituta);
    }
}
