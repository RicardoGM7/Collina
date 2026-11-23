// Tipagens de Usuário, Perfis/Roles e credenciais

// Mesmos valores de PerfilUsuarioEnum no back-end (tb_usuario).
export type PerfilUsuario =
  | 'ADMIN'
  | 'RECEPCAO'
  | 'FINANCEIRO'
  | 'ESTOQUE'
  | 'ESCALA_RH'
  | 'ASSOCIADO'

export const PERFIS: readonly PerfilUsuario[] = [
  'ADMIN',
  'RECEPCAO',
  'FINANCEIRO',
  'ESTOQUE',
  'ESCALA_RH',
  'ASSOCIADO',
]

export const PERFIL_LABEL: Record<PerfilUsuario, string> = {
  ADMIN: 'Administrador',
  RECEPCAO: 'Funcionário de Recepção',
  FINANCEIRO: 'Funcionário Financeiro',
  ESTOQUE: 'Funcionário de Estoque',
  ESCALA_RH: 'Funcionário de Escala/RH',
  ASSOCIADO: 'Associado',
}

// Tela inicial de cada perfil (para onde o usuário vai após o login).
// Cada setor começa direto no seu módulo; troque por um painel próprio quando ele existir.
export const HOME_POR_PERFIL: Record<PerfilUsuario, string> = {
  ADMIN: '/admin',
  RECEPCAO: '/recepcao',
  FINANCEIRO: '/financeiro',
  ESTOQUE: '/estoque',
  ESCALA_RH: '/escala',
  ASSOCIADO: '/associado',
}

export function isPerfil(valor: unknown): valor is PerfilUsuario {
  return typeof valor === 'string' && (PERFIS as readonly string[]).includes(valor)
}

// Usuário logado, como o front enxerga
export interface Usuario {
  uid: string
  email: string
  nome: string
  perfil: PerfilUsuario
  emailVerificado: boolean
}

// Resposta da API (UsuarioResponseDTO) em /api/auth/*
export interface UsuarioApi {
  uid: string
  email: string
  nome: string | null
  perfil: PerfilUsuario | null
  ativo: boolean
  emailVerificado: boolean
}

export interface Credenciais {
  email: string
  senha: string
}
