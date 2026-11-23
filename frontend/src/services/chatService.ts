import { api } from './api'

export interface AcaoPendenteDTO {
  acaoId: string
  titulo: string
  descricao: string
  detalhes: string[]
  critica: boolean
}

export interface ResultadoAcaoDTO {
  acaoId: string
  status: 'CONFIRMADA' | 'CANCELADA' | 'EXPIRADA' | 'ERRO' | string
  mensagem: string
  erro: boolean
}

export interface ChatRequestPayload {
  mensagem: string
  conversaId?: string | null
}

export interface ChatResponseDTO {
  mensagem: string
  conversaId: string
  erro?: boolean
  acoesPendentes?: AcaoPendenteDTO[]
}

export const chatService = {
  async enviarMensagem(
    mensagem: string,
    conversaId?: string | null,
  ): Promise<ChatResponseDTO> {
    const payload: ChatRequestPayload = conversaId
      ? { mensagem, conversaId }
      : { mensagem }

    // Timeout estendido (120s) para comportar chamadas encadeadas de tools do Gemini
    const { data } = await api.post<ChatResponseDTO>('/api/chat', payload, {
      timeout: 120000,
    })
    return data
  },

  async confirmarAcao(acaoId: string): Promise<ResultadoAcaoDTO> {
    const { data } = await api.post<ResultadoAcaoDTO>(
      `/api/chat/acoes/${encodeURIComponent(acaoId)}/confirmar`,
    )
    return data
  },

  async cancelarAcao(acaoId: string): Promise<ResultadoAcaoDTO> {
    const { data } = await api.post<ResultadoAcaoDTO>(
      `/api/chat/acoes/${encodeURIComponent(acaoId)}/cancelar`,
    )
    return data
  },

  async limparConversa(conversaId: string): Promise<void> {
    await api.delete(`/api/chat/${encodeURIComponent(conversaId)}`)
  },
}
