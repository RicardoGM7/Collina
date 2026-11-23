// Traduz erros do Firebase Auth, da API (Axios) e erros comuns em mensagens para o usuário
import axios from 'axios'
import { FirebaseError } from 'firebase/app'

const MENSAGEM_LOGIN_GENERICA = 'E-mail ou senha incorretos.'

const MENSAGENS_FIREBASE: Record<string, string> = {
  'auth/invalid-credential': MENSAGEM_LOGIN_GENERICA,
  'auth/invalid-login-credentials': MENSAGEM_LOGIN_GENERICA,
  'auth/wrong-password': MENSAGEM_LOGIN_GENERICA,
  'auth/user-not-found': MENSAGEM_LOGIN_GENERICA,
  'auth/user-disabled': MENSAGEM_LOGIN_GENERICA,
  'auth/invalid-email': 'Informe um e-mail válido.',
  'auth/too-many-requests': 'Muitas tentativas. Aguarde alguns minutos e tente novamente.',
  'auth/email-already-in-use': 'Já existe uma conta com este e-mail.',
  'auth/weak-password': 'A senha deve ter pelo menos 6 caracteres.',
  'auth/network-request-failed': 'Falha de conexão. Verifique sua internet e tente novamente.',
  'auth/invalid-api-key': 'Firebase não configurado corretamente (API key inválida).',
}

export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof FirebaseError) {
    return MENSAGENS_FIREBASE[erro.code] ?? 'Não foi possível concluir a operação. Tente novamente.'
  }

  if (axios.isAxiosError(erro)) {
    if (!erro.response) {
      return 'Não foi possível conectar ao servidor do clube. Verifique se a API está em execução.'
    }
    if (erro.response.status === 401) {
      return 'Sessão inválida ou expirada. Faça login novamente.'
    }
    const mensagem = (erro.response.data as { message?: string } | undefined)?.message
    return mensagem ?? 'O servidor retornou um erro. Tente novamente.'
  }

  if (erro instanceof Error) {
    return erro.message
  }
  return 'Ocorreu um erro inesperado.'
}

// No login, qualquer rejeição por credencial ou situação cadastral (ex.: conta inativa)
// deve exibir a mesma mensagem genérica, sem revelar o motivo específico.
export function mensagemDeErroLogin(erro: unknown): string {
  if (axios.isAxiosError(erro)) {
    if (!erro.response) {
      return 'Não foi possível conectar ao servidor do clube. Verifique se a API está em execução.'
    }
    if (erro.response.status === 401 || erro.response.status === 403) {
      return MENSAGEM_LOGIN_GENERICA
    }
  }
  return mensagemDeErro(erro)
}
