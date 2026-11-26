package br.ufs.dcomp.collina.service.espacos;

import br.ufs.dcomp.collina.dto.associados.AssociadoResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.associados.Associado;
import br.ufs.dcomp.collina.model.associados.Pessoa;
import br.ufs.dcomp.collina.model.associados.Plano;
import br.ufs.dcomp.collina.model.associados.SituacaoCadastralEnum;
import br.ufs.dcomp.collina.model.associados.StatusVinculoEnum;
import br.ufs.dcomp.collina.model.associados.VinculoDependencia;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Repositórios em memória para testar os services sem banco e sem Mockito.
 * Simulam a restrição única de reserva ativa (DataIntegrityViolationException).
 */
final class EspacosFakes {

    private EspacosFakes() {
    }

    // "Hoje" fixo: 19/11/2026 às 12:00 em Brasília
    static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-19T15:00:00Z"), ReservaRegras.FUSO);

    static class EspacoRepoFake implements EspacoFisicoRepository {
        final Map<Integer, EspacoFisico> dados = new HashMap<>();
        int proximoId = 100;

        @Override
        public EspacoFisico save(EspacoFisico e) {
            if (e.getId() == null) {
                e.setId(proximoId++);
            }
            dados.put(e.getId(), e);
            return e;
        }

        @Override
        public Optional<EspacoFisico> findById(Integer id) {
            return Optional.ofNullable(dados.get(id));
        }

        @Override
        public List<EspacoFisico> findAllByOrderByNomeAsc() {
            return dados.values().stream().sorted(Comparator.comparing(EspacoFisico::getNome)).toList();
        }

        @Override
        public boolean existsByNomeIgnoreCase(String nome) {
            return dados.values().stream().anyMatch(e -> e.getNome().equalsIgnoreCase(nome));
        }

        @Override
        public boolean existsByNomeIgnoreCaseAndIdNot(String nome, Integer id) {
            return dados.values().stream().anyMatch(e -> e.getNome().equalsIgnoreCase(nome) && !e.getId().equals(id));
        }

        @Override
        public Optional<EspacoFisico> findWithLockById(Integer id) {
            return findById(id);
        }

        @Override
        public void delete(EspacoFisico espaco) {
            dados.remove(espaco.getId());
        }
    }

    static class BlocoRepoFake implements BlocoHorarioRepository {
        final List<BlocoHorario> dados = new ArrayList<>();
        int proximoId = 500;

        @Override
        public BlocoHorario save(BlocoHorario b) {
            if (b.getId() == null) {
                b.setId(proximoId++);
                dados.add(b);
            }
            return b;
        }

        @Override
        public List<BlocoHorario> findByEspacoIdOrderByHoraInicioAsc(Integer espacoId) {
            return dados.stream()
                    .filter(b -> b.getEspaco().getId().equals(espacoId))
                    .sorted(Comparator.comparing(BlocoHorario::getHoraInicio))
                    .toList();
        }

        @Override
        public List<BlocoHorario> findByEspacoIdAndIdIn(Integer espacoId, Collection<Integer> ids) {
            return dados.stream()
                    .filter(b -> b.getEspaco().getId().equals(espacoId) && ids.contains(b.getId()))
                    .toList();
        }

        @Override
        public Optional<BlocoHorario> findByIdAndEspacoId(Integer id, Integer espacoId) {
            return dados.stream()
                    .filter(b -> b.getId().equals(id) && b.getEspaco().getId().equals(espacoId))
                    .findFirst();
        }

        @Override
        public void delete(BlocoHorario bloco) {
            dados.remove(bloco);
        }

        @Override
        public void deleteByEspacoId(Integer espacoId) {
            dados.removeIf(b -> b.getEspaco().getId().equals(espacoId));
        }
    }

    static class ReservaRepoFake implements ReservaRepository {
        final List<Reserva> dados = new ArrayList<>();
        int proximoId = 900;

        @Override
        public Reserva saveAndFlush(Reserva r) {
            boolean duplicada = dados.stream().anyMatch(x ->
                    x != r
                            && x.getStatus() == StatusReservaEnum.CONFIRMADA
                            && r.getStatus() == StatusReservaEnum.CONFIRMADA
                            && x.getEspaco().getId().equals(r.getEspaco().getId())
                            && x.getBloco().getId().equals(r.getBloco().getId())
                            && x.getDataUso().equals(r.getDataUso()));
            if (duplicada) {
                throw new DataIntegrityViolationException("ux_reserva_slot_ativo");
            }
            return save(r);
        }

        @Override
        public Reserva save(Reserva r) {
            if (r.getId() == null) {
                r.setId(proximoId++);
                dados.add(r);
            }
            return r;
        }

        @Override
        public Optional<Reserva> findWithLockById(Integer id) {
            return dados.stream().filter(r -> r.getId().equals(id)).findFirst();
        }

        @Override
        public List<Reserva> findByTitularId(Integer titularId) {
            return dados.stream().filter(r -> r.getTitular().getId().equals(titularId)).toList();
        }

        @Override
        public List<Reserva> findByEspacoIdAndDataUsoAndStatus(Integer espacoId, LocalDate data, StatusReservaEnum status) {
            return dados.stream()
                    .filter(r -> r.getEspaco().getId().equals(espacoId)
                            && r.getDataUso().equals(data) && r.getStatus() == status)
                    .toList();
        }

        @Override
        public List<Reserva> findByEspacoIdAndDataUsoAndStatusAndBlocoIdIn(
                Integer espacoId, LocalDate data, StatusReservaEnum status, Collection<Integer> blocoIds) {
            return findByEspacoIdAndDataUsoAndStatus(espacoId, data, status).stream()
                    .filter(r -> blocoIds.contains(r.getBloco().getId()))
                    .toList();
        }

        @Override
        public long countByTitularIdAndStatusAndDataUsoGreaterThanEqualAndDataUsoLessThan(
                Integer titularId, StatusReservaEnum status, LocalDate inicio, LocalDate fim) {
            return dados.stream()
                    .filter(r -> r.getTitular().getId().equals(titularId) && r.getStatus() == status
                            && !r.getDataUso().isBefore(inicio) && r.getDataUso().isBefore(fim))
                    .count();
        }

        @Override
        public boolean existsByEspacoId(Integer espacoId) {
            return dados.stream().anyMatch(r -> r.getEspaco().getId().equals(espacoId));
        }

        @Override
        public boolean existsByBlocoId(Integer blocoId) {
            return dados.stream().anyMatch(r -> r.getBloco().getId().equals(blocoId));
        }
    }

    static class AssociadoLockRepoFake implements AssociadoReservaRepository {
        final Map<Integer, Associado> dados = new HashMap<>();

        @Override
        public Optional<Associado> findWithLockById(Integer id) {
            return Optional.ofNullable(dados.get(id));
        }
    }

    /** Cenário completo: espaços, blocos, titulares, dependentes e todos os fakes ligados. */
    static class Cenario {
        final EspacoRepoFake espacos = new EspacoRepoFake();
        final BlocoRepoFake blocos = new BlocoRepoFake();
        final ReservaRepoFake reservas = new ReservaRepoFake();
        final AssociadoLockRepoFake associados = new AssociadoLockRepoFake();
        final Map<Integer, VinculoDependencia> vinculos = new HashMap<>();
        final Map<String, Integer> uidParaTitular = new HashMap<>();
        final Set<Integer> inadimplentes = new HashSet<>();

        EspacoFisico quadra; // controlada: capacidade 10, convidados até 5
        EspacoFisico piscina; // pública: convidados até 20
        BlocoHorario b0810;
        BlocoHorario b1012;
        BlocoHorario b1416;
        BlocoHorario bManutencao; // 16-18, MANUTENCAO
        BlocoHorario bTreino; // 18-20, TREINO
        BlocoHorario bPiscina;

        Cenario() {
            quadra = novoEspaco("Quadra 1", false, 5, 10);
            piscina = novoEspaco("Piscina", true, 20, null);
            b0810 = novoBloco(quadra, "08:00", "10:00", TipoBlocoEnum.LIVRE);
            b1012 = novoBloco(quadra, "10:00", "12:00", TipoBlocoEnum.LIVRE);
            b1416 = novoBloco(quadra, "14:00", "16:00", TipoBlocoEnum.LIVRE);
            bManutencao = novoBloco(quadra, "16:00", "18:00", TipoBlocoEnum.MANUTENCAO);
            bTreino = novoBloco(quadra, "18:00", "20:00", TipoBlocoEnum.TREINO);
            bPiscina = novoBloco(piscina, "08:00", "10:00", TipoBlocoEnum.LIVRE);

            novoTitular(1, "uid-1", 4);
            novoTitular(2, "uid-2", 4);
            novoVinculo(100, 1, "2015-05-10", StatusVinculoEnum.ATIVO); // criança
            novoVinculo(101, 1, "2005-01-01", StatusVinculoEnum.ATIVO); // já passou dos 21
            novoVinculo(102, 2, "2015-05-10", StatusVinculoEnum.ATIVO); // de outro titular
            novoVinculo(103, 1, "2015-05-10", StatusVinculoEnum.INATIVO); // vínculo inativo
        }

        EspacoFisico novoEspaco(String nome, boolean publico, int limiteConvidados, Integer capacidade) {
            EspacoFisico e = new EspacoFisico();
            e.setNome(nome);
            e.setPublico(publico);
            e.setLimiteConvidados(limiteConvidados);
            e.setCapacidadeMaxima(capacidade);
            return espacos.save(e);
        }

        BlocoHorario novoBloco(EspacoFisico espaco, String ini, String fim, TipoBlocoEnum tipo) {
            BlocoHorario b = new BlocoHorario();
            b.setEspaco(espaco);
            b.setHoraInicio(LocalTime.parse(ini));
            b.setHoraFim(LocalTime.parse(fim));
            b.setTipo(tipo);
            return blocos.save(b);
        }

        Associado novoTitular(int id, String uid, int limiteMensal) {
            Plano plano = new Plano();
            plano.setLimiteMensalReservas(limiteMensal);
            Pessoa pessoa = new Pessoa();
            pessoa.setNome("Titular " + id);
            pessoa.setDataNascimento(LocalDate.parse("1985-03-15"));
            Associado a = new Associado();
            a.setId(id);
            a.setPessoa(pessoa);
            a.setPlano(plano);
            a.setSituacaoCadastral(SituacaoCadastralEnum.ATIVO);
            associados.dados.put(id, a);
            uidParaTitular.put(uid, id);
            return a;
        }

        VinculoDependencia novoVinculo(int id, int titularId, String nascimento, StatusVinculoEnum status) {
            Pessoa dependente = new Pessoa();
            dependente.setNome("Dependente " + id);
            dependente.setDataNascimento(LocalDate.parse(nascimento));
            VinculoDependencia v = new VinculoDependencia();
            v.setId(id);
            v.setTitular(associados.dados.get(titularId));
            v.setDependente(dependente);
            v.setStatus(status);
            vinculos.put(id, v);
            return v;
        }

        Associado titular(int id) {
            return associados.dados.get(id);
        }

        AssociadoServiceFake associadoService() {
            return new AssociadoServiceFake(this);
        }

        // VinculoDependenciaRepository estende JpaRepository (dezenas de métodos): proxy só com findById
        VinculoDependenciaRepository vinculoRepo() {
            return (VinculoDependenciaRepository) Proxy.newProxyInstance(
                    VinculoDependenciaRepository.class.getClassLoader(),
                    new Class<?>[]{VinculoDependenciaRepository.class},
                    (proxy, metodo, args) -> {
                        if (metodo.getName().equals("findById")) {
                            return Optional.ofNullable(vinculos.get((Integer) args[0]));
                        }
                        throw new UnsupportedOperationException(metodo.getName());
                    });
        }

        ReservaService reservaService() {
            return reservaService(7);
        }

        ReservaService reservaService(int maxDias) {
            return new ReservaService(associadoService(), associados, vinculoRepo(), espacos, blocos, reservas,
                    inadimplentes::contains, RELOGIO, maxDias);
        }

        EspacoService espacoService() {
            return new EspacoService(espacos, blocos, reservas);
        }

        EspacoFisico estado(EstadoEspacoEnum estado) {
            quadra.setEstado(estado);
            return quadra;
        }
    }

    /** AssociadoService sem banco: resolve uid -> titular pelo cenário. */
    static class AssociadoServiceFake extends br.ufs.dcomp.collina.service.associados.AssociadoService {
        private final Cenario cenario;

        AssociadoServiceFake(Cenario cenario) {
            this.cenario = cenario;
        }

        @Override
        public Integer obterIdPorUsuarioId(String usuarioId) {
            Integer id = cenario.uidParaTitular.get(usuarioId);
            if (id == null) {
                throw new ResourceNotFoundException("Nenhum cadastro de associado vinculado ao usuário autenticado.");
            }
            return id;
        }

        @Override
        public AssociadoResponseDTO buscarPorId(Integer id) {
            if (!cenario.associados.dados.containsKey(id)) {
                throw new ResourceNotFoundException("Associado não encontrado.");
            }
            return null; // o service de reservas só usa a chamada para validar a existência
        }
    }
}
