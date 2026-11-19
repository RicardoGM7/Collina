import { api } from '../../../services/api'
import type {
  Associado,
  AssociadoRequest,
  AssociadoUpdateRequest,
  DependenteRequest,
  DependenteUpdateRequest,
  Plano,
  PlanoRequest,
  SituacaoCadastral,
  StatusVinculo,
  TipoPlano,
  VinculoDependencia,
} from '../types'

export const associadosApi = {
  listar: (situacao?: SituacaoCadastral) =>
    api.get<Associado[]>('/associados', { params: situacao ? { situacao } : undefined }).then((r) => r.data),

  buscarPorId: (id: number) => api.get<Associado>(`/associados/${id}`).then((r) => r.data),

  cadastrar: (dados: AssociadoRequest) => api.post<Associado>('/associados', dados).then((r) => r.data),

  atualizarDadosPessoais: (id: number, dados: AssociadoUpdateRequest) =>
    api.put<Associado>(`/associados/${id}`, dados).then((r) => r.data),

  alterarPlano: (id: number, tipoPlano: TipoPlano) =>
    api.patch<Associado>(`/associados/${id}/plano`, { tipoPlano }).then((r) => r.data),

  alterarSituacao: (id: number, situacao: SituacaoCadastral) =>
    api.patch<Associado>(`/associados/${id}/situacao`, { situacao }).then((r) => r.data),
}

export const dependentesApi = {
  listarPorTitular: (titularId: number, status?: StatusVinculo) =>
    api
      .get<VinculoDependencia[]>(`/associados/${titularId}/dependentes`, { params: status ? { status } : undefined })
      .then((r) => r.data),

  cadastrar: (titularId: number, dados: DependenteRequest) =>
    api.post<VinculoDependencia>(`/associados/${titularId}/dependentes`, dados).then((r) => r.data),

  atualizar: (vinculoId: number, dados: DependenteUpdateRequest) =>
    api.put<VinculoDependencia>(`/dependentes/${vinculoId}`, dados).then((r) => r.data),

  inativar: (vinculoId: number) =>
    api.patch<VinculoDependencia>(`/dependentes/${vinculoId}/inativar`).then((r) => r.data),

  // RF-AS04: inativa os vínculos de quem já completou 21 anos
  inativarPorIdade: () =>
    api.post<{ vinculosInativados: number }>('/dependentes/inativar-por-idade').then((r) => r.data),
}

export const planosApi = {
  listar: () => api.get<Plano[]>('/planos').then((r) => r.data),

  atualizar: (id: number, dados: PlanoRequest) => api.put<Plano>(`/planos/${id}`, dados).then((r) => r.data),
}
