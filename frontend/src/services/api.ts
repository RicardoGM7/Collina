import axios from 'axios'

export interface ApiErrorPayload {
  timestamp?: string
  status?: number
  error?: string
  message?: string
  path?: string
  fieldErrors?: Record<string, string>
}

const baseURL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080')
  .replace(/\/+$/, '')
  .replace(/\/api$/, '')

export const api = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
})

export function getApiErrorMessage(
  error: unknown,
  fallback = 'Não foi possível completar a operação.'
): string {
  if (axios.isAxiosError<ApiErrorPayload>(error)) {
    const data = error.response?.data

    if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
      const firstFieldMessage = Object.values(data.fieldErrors)[0]
      if (firstFieldMessage) return firstFieldMessage
    }

    if (data?.message) {
      return data.message
    }

    if (error.code === 'ERR_NETWORK' || !error.response) {
      return 'Não foi possível conectar ao servidor. Verifique se o backend está rodando na porta 8080.'
    }
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}
