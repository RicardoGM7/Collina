// Cadastro do associado autenticado (GET /api/associados/me), compartilhado entre o portal e o modal de perfil.
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'
import { useAuth } from '../../context/AuthContext'
import { associadoService } from '../../services/associadoService'
import type { Associado } from '../../types/associado'
import { mensagemErroApi, statusHttp } from './associadoUtils'

interface MeuCadastroValue {
  associado: Associado | null
  carregando: boolean
  /** true quando a conta logada ainda não foi vinculada a um titular pela administração (404) */
  naoVinculado: boolean
  erro: string
  recarregar: () => Promise<void>
  atualizarLocal: (associado: Associado) => void
}

const MeuCadastroContext = createContext<MeuCadastroValue | null>(null)

export function MeuCadastroProvider({ children }: { children: ReactNode }) {
  const { usuario } = useAuth()
  const ehAssociado = usuario?.perfil === 'ASSOCIADO'

  const [associado, setAssociado] = useState<Associado | null>(null)
  const [carregando, setCarregando] = useState(ehAssociado)
  const [naoVinculado, setNaoVinculado] = useState(false)
  const [erro, setErro] = useState('')

  const recarregar = useCallback(async () => {
    if (!ehAssociado) return
    setCarregando(true)
    setErro('')
    setNaoVinculado(false)
    try {
      setAssociado(await associadoService.meuCadastro())
    } catch (e) {
      setAssociado(null)
      if (statusHttp(e) === 404) setNaoVinculado(true)
      else setErro(mensagemErroApi(e, 'Não foi possível carregar seu cadastro.'))
    } finally {
      setCarregando(false)
    }
  }, [ehAssociado])

  useEffect(() => {
    void recarregar()
  }, [recarregar])

  return (
    <MeuCadastroContext.Provider
      value={{ associado, carregando, naoVinculado, erro, recarregar, atualizarLocal: setAssociado }}
    >
      {children}
    </MeuCadastroContext.Provider>
  )
}

export function useMeuCadastro(): MeuCadastroValue {
  const ctx = useContext(MeuCadastroContext)
  if (!ctx) throw new Error('useMeuCadastro deve ser usado dentro de MeuCadastroProvider')
  return ctx
}
