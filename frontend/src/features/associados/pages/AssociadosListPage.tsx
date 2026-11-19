import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AssociadoFormModal from '../components/AssociadoFormModal'
import Aviso from '../components/Aviso'
import Badge, { varianteSituacao } from '../components/Badge'
import { associadosApi, dependentesApi } from '../services/associadosApi'
import type { Associado, SituacaoCadastral } from '../types'
import {
  CATEGORIA_LABEL,
  SITUACAO_LABEL,
  TIPO_PLANO_LABEL,
  formatarCpf,
  formatarData,
  mensagemDeErro,
  somenteDigitos,
} from '../utils'

export default function AssociadosListPage() {
  const navigate = useNavigate()
  const [associados, setAssociados] = useState<Associado[]>([])
  const [situacao, setSituacao] = useState<SituacaoCadastral | ''>('')
  const [busca, setBusca] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [novoAberto, setNovoAberto] = useState(false)
  const [verificandoIdade, setVerificandoIdade] = useState(false)

  const carregar = useCallback(async () => {
    setCarregando(true)
    setErro(null)
    try {
      setAssociados(await associadosApi.listar(situacao || undefined))
    } catch (e) {
      setErro(mensagemDeErro(e))
    } finally {
      setCarregando(false)
    }
  }, [situacao])

  useEffect(() => {
    void carregar()
  }, [carregar])

  const filtrados = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    if (!termo) return associados
    const digitos = somenteDigitos(termo)
    return associados.filter(
      (a) => a.pessoa.nome.toLowerCase().includes(termo) || (digitos !== '' && a.pessoa.cpf.includes(digitos)),
    )
  }, [associados, busca])

  // RF-AS04: dispara a inativação dos vínculos de dependentes que já completaram 21 anos
  const verificarIdadeDependentes = async () => {
    setVerificandoIdade(true)
    setErro(null)
    setAviso(null)
    try {
      const { vinculosInativados } = await dependentesApi.inativarPorIdade()
      setAviso(
        vinculosInativados === 0
          ? 'Nenhum dependente atingiu 21 anos.'
          : `${vinculosInativados} vínculo(s) de dependência inativado(s) por idade.`,
      )
    } catch (e) {
      setErro(mensagemDeErro(e))
    } finally {
      setVerificandoIdade(false)
    }
  }

  return (
    <section>
      <div className="as-titulo-linha">
        <h1>Associados titulares</h1>
        <div className="as-acoes">
          <button
            type="button"
            className="as-btn as-btn-secundario"
            onClick={verificarIdadeDependentes}
            disabled={verificandoIdade}
            title="Inativa os vínculos de dependentes que completaram 21 anos (RF-AS04)"
          >
            {verificandoIdade ? 'Verificando…' : 'Verificar dependentes com 21 anos'}
          </button>
          <button type="button" className="as-btn as-btn-primario" onClick={() => setNovoAberto(true)}>
            + Novo associado
          </button>
        </div>
      </div>

      <Aviso tipo="erro" mensagem={erro} onFechar={() => setErro(null)} />
      <Aviso tipo="sucesso" mensagem={aviso} onFechar={() => setAviso(null)} />

      <div className="as-filtros">
        <input
          type="search"
          placeholder="Buscar por nome ou CPF"
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
        />
        <select value={situacao} onChange={(e) => setSituacao(e.target.value as SituacaoCadastral | '')}>
          <option value="">Todas as situações</option>
          {(Object.keys(SITUACAO_LABEL) as SituacaoCadastral[]).map((s) => (
            <option key={s} value={s}>
              {SITUACAO_LABEL[s]}
            </option>
          ))}
        </select>
      </div>

      <div className="as-tabela-wrap">
        <table className="as-tabela">
          <thead>
            <tr>
              <th>Nome</th>
              <th>CPF</th>
              <th>Categoria</th>
              <th>Plano</th>
              <th>Situação</th>
              <th>Admissão</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {carregando && (
              <tr>
                <td colSpan={7} className="as-vazio">
                  Carregando…
                </td>
              </tr>
            )}
            {!carregando && filtrados.length === 0 && (
              <tr>
                <td colSpan={7} className="as-vazio">
                  Nenhum associado encontrado.
                </td>
              </tr>
            )}
            {!carregando &&
              filtrados.map((a) => (
                <tr key={a.id} className="as-linha-clicavel" onClick={() => navigate(`/associados/${a.id}`)}>
                  <td>{a.pessoa.nome}</td>
                  <td>{formatarCpf(a.pessoa.cpf)}</td>
                  <td>{CATEGORIA_LABEL[a.categoria]}</td>
                  <td>{TIPO_PLANO_LABEL[a.plano.tipo]}</td>
                  <td>
                    <Badge texto={SITUACAO_LABEL[a.situacaoCadastral]} variante={varianteSituacao(a.situacaoCadastral)} />
                  </td>
                  <td>{formatarData(a.dataAdmissao)}</td>
                  <td>
                    <Link to={`/associados/${a.id}`} onClick={(e) => e.stopPropagation()}>
                      Gerenciar
                    </Link>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {novoAberto && (
        <AssociadoFormModal
          onFechar={() => setNovoAberto(false)}
          onCadastrado={(novo) => {
            setNovoAberto(false)
            navigate(`/associados/${novo.id}`)
          }}
        />
      )}
    </section>
  )
}
