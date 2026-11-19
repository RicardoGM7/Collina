<<<<<<< HEAD
import axios from 'axios'

// Instância única do Axios usada por todos os módulos.
// O endereço da API pode ser trocado criando um arquivo frontend/.env.local com:
//   VITE_API_URL=http://localhost:8080/api
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
})

// TODO (módulo de autenticação): adicionar aqui o interceptor que envia o token do Firebase
// no cabeçalho Authorization. Hoje o backend libera todas as rotas (SecurityConfig).
=======
// Instância do Axios com interceptor de autenticação
>>>>>>> origin/main
