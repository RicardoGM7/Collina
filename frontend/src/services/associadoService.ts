// Comunicação com o módulo de associados do back-end. Sem dados simulados: erros sobem para a tela.
import { api } from './api'
import type {
  Associado,
  CategoriaAssociado,
  Plano,
  SituacaoCadastral,
  StatusVinculo,
  TipoPlano,
  VinculoDependencia,
} from '../types/associado'

export interface CadastrarAssociadoDTO {
  nome: string
  cpf: string
  dataNascimento: string
  categoria: CategoriaAssociado
  tipoPlano: TipoPlano
  usuarioId?: string
  email?: string
  senha?: string
}

export interface CadastrarDependenteDTO {
  nome: string
  cpf: string
  dataNascimento: string
  parentesco: string
}

export interface AtualizarDadosPessoaisDTO {
  nome: string
  dataNascimento: string
}

export interface ParametrosPlanoDTO {
  valorPeriodico: number
  limiteMensalReservas: number
  maxDependentesAtivos: number
}

// BigDecimal chega como número (ou string, conforme a serialização); normaliza para number
function normalizarPlano(p: Plano): Plano {
  return { ...p, valorPeriodico: Number(p.valorPeriodico) }
}

function normalizarAssociado(a: Associado): Associado {
  return { ...a, plano: normalizarPlano(a.plano) }
}

export const associadoService = {
  // ===== Associado autenticado (perfil ASSOCIADO) =====

  async meuCadastro(): Promise<Associado> {
    const { data } = await api.get<Associado>('/api/associados/me')
    return normalizarAssociado(data)
  },

  async cadastrarMeuPerfil(
    dto: Omit<CadastrarAssociadoDTO, 'usuarioId' | 'email' | 'senha'>,
  ): Promise<Associado> {
    const { data } = await api.post<Associado>('/api/associados/me', {
      ...dto,
      cpf: dto.cpf.replace(/\D/g, ''),
    })
    return normalizarAssociado(data)
  },

  async atualizarMeusDados(dto: AtualizarDadosPessoaisDTO): Promise<Associado> {
    const { data } = await api.put<Associado>('/api/associados/me', dto)
    return normalizarAssociado(data)
  },

  async meusDependentes(status?: StatusVinculo): Promise<VinculoDependencia[]> {
    const { data } = await api.get<VinculoDependencia[]>('/api/associados/me/dependentes', {
      params: status ? { status } : undefined,
    })
    return data
  },

  // ===== Administração / recepção =====

  async listar(situacao?: SituacaoCadastral): Promise<Associado[]> {
    const { data } = await api.get<Associado[]>('/api/associados', {
      params: situacao ? { situacao } : undefined,
    })
    return data.map(normalizarAssociado)
  },

  async cadastrar(dto: CadastrarAssociadoDTO): Promise<Associado> {
    const { data } = await api.post<Associado>('/api/associados', {
      ...dto,
      cpf: dto.cpf.replace(/\D/g, ''),
    })
    return normalizarAssociado(data)
  },

  async alterarPlano(id: number, tipoPlano: TipoPlano): Promise<Associado> {
    const { data } = await api.patch<Associado>(`/api/associados/${id}/plano`, { tipoPlano })
    return normalizarAssociado(data)
  },

  async alterarSituacao(id: number, situacao: SituacaoCadastral): Promise<Associado> {
    const { data } = await api.patch<Associado>(`/api/associados/${id}/situacao`, { situacao })
    return normalizarAssociado(data)
  },

  async listarDependentes(titularId: number): Promise<VinculoDependencia[]> {
    const { data } = await api.get<VinculoDependencia[]>(
      `/api/associados/${titularId}/dependentes`,
    )
    return data
  },

  async cadastrarDependente(
    titularId: number,
    dto: CadastrarDependenteDTO,
  ): Promise<VinculoDependencia> {
    const { data } = await api.post<VinculoDependencia>(
      `/api/associados/${titularId}/dependentes`,
      { ...dto, cpf: dto.cpf.replace(/\D/g, '') },
    )
    return data
  },

  async inativarDependente(vinculoId: number): Promise<VinculoDependencia> {
    const { data } = await api.patch<VinculoDependencia>(
      `/api/dependentes/${vinculoId}/inativar`,
    )
    return data
  },

  // RF-AS04: dispara a rotina de inativação por idade (21 anos)
  async inativarDependentesPorIdade(): Promise<number> {
    const { data } = await api.post<{ vinculosInativados: number }>(
      '/api/dependentes/inativar-por-idade',
    )
    return data.vinculosInativados
  },

  // ===== Planos (RF-AS02) =====

  async listarPlanos(): Promise<Plano[]> {
    const { data } = await api.get<Plano[]>('/api/planos')
    return data.map(normalizarPlano)
  },

  async atualizarPlano(id: number, dto: ParametrosPlanoDTO): Promise<Plano> {
    const { data } = await api.put<Plano>(`/api/planos/${id}`, dto)
    return normalizarPlano(data)
  },
}
