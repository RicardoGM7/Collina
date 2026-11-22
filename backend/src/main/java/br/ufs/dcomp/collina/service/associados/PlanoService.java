package br.ufs.dcomp.collina.service.associados;

import br.ufs.dcomp.collina.dto.associados.PlanoRequestDTO;
import br.ufs.dcomp.collina.dto.associados.PlanoResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.associados.Plano;
import br.ufs.dcomp.collina.model.associados.TipoPlanoEnum;
import br.ufs.dcomp.collina.repository.associados.PlanoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PlanoService {

    @Autowired
    private PlanoRepository planoRepository;

    // 1. Listar todos os planos
    @Transactional(readOnly = true)
    public List<PlanoResponseDTO> listarTodos() {
        return planoRepository.findAll()
                .stream()
                .map(PlanoResponseDTO::new)
                .toList();
    }

    // 2. Buscar plano por ID
    @Transactional(readOnly = true)
    public PlanoResponseDTO buscarPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do plano é obrigatório.");
        }
        return new PlanoResponseDTO(obterPlano(id));
    }

    // 3. Buscar plano por tipo
    @Transactional(readOnly = true)
    public PlanoResponseDTO buscarPorTipo(TipoPlanoEnum tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo do plano é obrigatório.");
        }
        Plano plano = planoRepository.findByTipo(tipo)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + tipo));
        return new PlanoResponseDTO(plano);
    }

    // 4. RF-AS02: atualizar os parâmetros do plano (valor, limite de reservas, máx. de dependentes ativos).
    // O tipo do plano não muda. Reduzir o máximo de dependentes não inativa vínculos já existentes:
    // apenas impede novos cadastros enquanto o titular estiver acima do limite.
    @Transactional
    public PlanoResponseDTO atualizarParametros(Integer id, PlanoRequestDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do plano é obrigatório.");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Os parâmetros do plano são obrigatórios.");
        }
        if (dto.valorPeriodico() == null || dto.valorPeriodico().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O valor periódico não pode ser nulo ou negativo.");
        }
        if (dto.valorPeriodico().stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("O valor periódico deve ter no máximo 2 casas decimais.");
        }
        if (dto.limiteMensalReservas() == null || dto.limiteMensalReservas() < 0) {
            throw new IllegalArgumentException("O limite mensal de reservas não pode ser nulo ou negativo.");
        }
        if (dto.maxDependentesAtivos() == null || dto.maxDependentesAtivos() < 0) {
            throw new IllegalArgumentException("A quantidade máxima de dependentes ativos não pode ser nula ou negativa.");
        }

        Plano plano = obterPlano(id);
        plano.setMensalidade(dto.valorPeriodico().setScale(2, RoundingMode.UNNECESSARY));
        plano.setLimiteMensalReservas(dto.limiteMensalReservas());
        plano.setMaxDependentesAtivos(dto.maxDependentesAtivos());

        return new PlanoResponseDTO(planoRepository.save(plano));
    }

    private Plano obterPlano(Integer id) {
        return planoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado com o ID: " + id));
    }
}
