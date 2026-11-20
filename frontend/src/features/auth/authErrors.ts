// Traduz erros do Firebase Auth, da API (Axios) e erros comuns em mensagens para o usuário
import axios from 'axios'
import { FirebaseError } from 'firebase/app'

const MENSAGENS_FIREBASE: Record<string, string> = {
  'auth/invalid-credential': 'E-mail ou senha incorretos.',
  'auth/invalid-login-credentials': 'E-mail ou senha incorretos.',
  'auth/wrong-password': 'E-mail ou senha incorretos.',
  'auth/user-not-found': 'E-mail ou senha incorretos.',
  'auth/invalid-email': 'Informe um e-mail válido.',
  'auth/user-disabled': 'Esta conta está desativada. Procure um administrador do clube.',
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
      return 'O servidor não aceitou sua autenticação. Confira se o back-end usa o mesmo projeto Firebase do front-end.'
    }
    const mensagem = (erro.response.data as { message?: string } | undefined)?.message
    return mensagem ?? 'O servidor retornou um erro. Tente novamente.'
  }

  if (erro instanceof Error) {
    return erro.message
  }
  return 'Ocorreu um erro inesperado.'
}
