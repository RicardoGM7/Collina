import { api } from './api'
import type { PerfilUsuario, UsuarioApi } from '../types/user'

export interface CriarUsuarioDTO {
  nome: string
  email: string
  senha: string
  perfil: PerfilUsuario
}

export const usuariosService = {
  async listar(): Promise<UsuarioApi[]> {
    const { data } = await api.get<UsuarioApi[]>('/api/auth/usuarios')
    return data
  },

  async criar(dto: CriarUsuarioDTO): Promise<UsuarioApi> {
    const { data } = await api.post<UsuarioApi>('/api/auth/usuarios', dto)
    return data
  },

  async alterarPerfil(
    uid: string,
    perfil: PerfilUsuario,
  ): Promise<UsuarioApi> {
    const { data } = await api.put<UsuarioApi>(
      `/api/auth/usuarios/${encodeURIComponent(uid)}/perfil`,
      { perfil },
    )
    return data
  },

  async alterarStatus(uid: string, ativo: boolean): Promise<UsuarioApi> {
    const { data } = await api.patch<UsuarioApi>(
      `/api/auth/usuarios/${encodeURIComponent(uid)}/status`,
      { ativo },
    )
    return data
  },
}
