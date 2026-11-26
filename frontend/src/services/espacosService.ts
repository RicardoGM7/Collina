// Comunicação com o módulo de espaços e reservas do back-end (br.ufs.dcomp.collina.dto.espacos).
// Sem dados simulados: erros sobem para a tela (use getApiErrorMessage).
import { api } from './api'

export type EstadoEspaco = 'ATIVO' | 'EM_MANUTENCAO' | 'INATIVO'
export type TipoBloco = 'LIVRE' | 'TREINO' | 'MANUTENCAO'
export type SituacaoBloco = 'LIVRE' | 'OCUPADO' | 'TREINO' | 'MANUTENCAO'
export type StatusReserva = 'CONFIRMADA' | 'CANCELADA'

// EspacoResponseDTO
export interface Espaco {
  id: number
  nome: string
  publico: boolean
  usoExclusivoTreino: boolean
  limiteConvidados: number
  capacidadeMaxima: number | null
  estado: EstadoEspaco
}

// EspacoRequestDTO
export interface EspacoPayload {
  nome: string
  publico: boolean
  usoExclusivoTreino: boolean
  limiteConvidados: number
  capacidadeMaxima: number | null
}

// BlocoResponseDTO (horários em HH:mm)
export interface Bloco {
  id: number
  espacoId: number
  horaInicio: string
  horaFim: string
  tipo: TipoBloco
}

// BlocoRequestDTO
export interface BlocoPayload {
  horaInicio: string
  horaFim: string
  tipo: TipoBloco
}

// DisponibilidadeResponseDTO.BlocoDisponibilidadeDTO
export interface BlocoDisponibilidade {
  blocoId: number
  horaInicio: string
  horaFim: string
  tipo: TipoBloco
  situacao: SituacaoBloco
}

// DisponibilidadeResponseDTO
export interface Disponibilidade {
  espacoId: number
  data: string
  blocos: BlocoDisponibilidade[]
  mensagem: string | null
}

// ReservaResponseDTO
export interface Reserva {
  id: number
  dataUso: string
  horaInicio: string
  horaFim: string
  espacoId: number
  espacoNome: string
  blocoId: number
  qtdConvidados: number
  status: StatusReserva
  titularId: number
  titularNome: string
  dependenteId: number | null
  dependenteNome: string | null
}

// ReservaRequestDTO (o titular vem do token, nunca do corpo)
export interface ReservaPayload {
  espacoId: number
  data: string
  blocoIds: number[]
  qtdConvidados: number
  dependenteId?: number | null
}

export const espacosService = {
  // ===== Espaços =====

  async listarEspacos(): Promise<Espaco[]> {
    const { data } = await api.get<Espaco[]>('/api/espacos')
    return data
  },

  async cadastrarEspaco(dto: EspacoPayload): Promise<Espaco> {
    const { data } = await api.post<Espaco>('/api/espacos', dto)
    return data
  },

  async atualizarEspaco(id: number, dto: EspacoPayload): Promise<Espaco> {
    const { data } = await api.put<Espaco>(`/api/espacos/${id}`, dto)
    return data
  },

  async alterarEstado(id: number, estado: EstadoEspaco): Promise<Espaco> {
    const { data } = await api.patch<Espaco>(`/api/espacos/${id}/estado`, { estado })
    return data
  },

  async removerEspaco(id: number): Promise<void> {
    await api.delete(`/api/espacos/${id}`)
  },

  // ===== Blocos e disponibilidade =====

  async listarBlocos(espacoId: number): Promise<Bloco[]> {
    const { data } = await api.get<Bloco[]>(`/api/espacos/${espacoId}/blocos`)
    return data
  },

  // Somente ADMIN
  async adicionarBloco(espacoId: number, dto: BlocoPayload): Promise<Bloco> {
    const { data } = await api.post<Bloco>(`/api/espacos/${espacoId}/blocos`, dto)
    return data
  },

  // Somente ADMIN
  async removerBloco(espacoId: number, blocoId: number): Promise<void> {
    await api.delete(`/api/espacos/${espacoId}/blocos/${blocoId}`)
  },

  // Associado recebe só os blocos livres; recepção/admin recebem a agenda completa
  async disponibilidade(espacoId: number, data: string): Promise<Disponibilidade> {
    const resposta = await api.get<Disponibilidade>(
      `/api/espacos/${espacoId}/disponibilidade`,
      { params: { data } },
    )
    return resposta.data
  },

  // ===== Reservas =====

  async solicitarReserva(dto: ReservaPayload): Promise<Reserva[]> {
    const { data } = await api.post<Reserva[]>('/api/reservas', dto)
    return data
  },

  async minhasReservas(): Promise<Reserva[]> {
    const { data } = await api.get<Reserva[]>('/api/reservas/minhas')
    return data
  },

  // Recepção e administrador
  async reservasDoAssociado(associadoId: number): Promise<Reserva[]> {
    const { data } = await api.get<Reserva[]>('/api/reservas', {
      params: { associadoId },
    })
    return data
  },

  async cancelarReserva(id: number): Promise<Reserva> {
    const { data } = await api.patch<Reserva>(`/api/reservas/${id}/cancelar`)
    return data
  },
}
