import { useState } from 'react'
import type { FormEvent } from 'react'
import { dependentesApi } from '../services/associadosApi'
import type { VinculoDependencia } from '../types'
import { IDADE_LIMITE_DEPENDENTE, hojeISO, idadeEm, mensagemDeErro, somenteDigitos } from '../utils'
import Aviso from './Aviso'
import Modal from './Modal'

interface Props {
  titularId: number
  // Quando informado, o modal edita o vínculo existente; caso contrário, cadastra um novo dependente
  vinculo?: VinculoDependencia
  onSalvo: () => void
  onFechar: () => void
}

// RF-AS03: cadastro e atualização de dependentes (feitos por funcionário)
export default function DependenteFormModal({ titularId, vinculo, onSalvo, onFechar }: Props) {
  const edicao = vinculo !== undefined
  const [nome, setNome] = useState(vinculo?.dependente.nome ?? '')
  const [cpf, setCpf] = useState(vinculo?.dependente.cpf ?? '')
  const [dataNascimento, setDataNascimento] = useState(vinculo?.dependente.dataNascimento ?? '')
  const [parentesco, setParentesco] = useState(vinculo?.parentesco ?? '')
  const [erro, setErro] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)

  const idade = dataNascimento ? idadeEm(dataNascimento) : null
  const acimaDoLimite = idade !== null && idade >= IDADE_LIMITE_DEPENDENTE

  const enviar = async (e: FormEvent) => {
    e.preventDefault()
    setErro(null)
    setSalvando(true)
    try {
      if (vinculo) {
        await dependentesApi.atualizar(vinculo.id, {
          nome: nome.trim(),
          dataNascimento,
          parentesco: parentesco.trim(),
        })
      } else {
        await dependentesApi.cadastrar(titularId, {
          nome: nome.trim(),
          cpf,
          dataNascimento,
          parentesco: parentesco.trim(),
        })
      }
      onSalvo()
    } catch (err) {
      setErro(mensagemDeErro(err))
    } finally {
      setSalvando(false)
    }
  }

  return (
    <Modal titulo={edicao ? 'Editar dependente' : 'Novo dependente'} onFechar={onFechar}>
      <form onSubmit={enviar} className="as-form">
        <Aviso tipo="erro" mensagem={erro} />

        <label>
          Nome completo
          <input value={nome} onChange={(e) => setNome(e.target.value)} required />
        </label>

        <div className="as-form-row">
          <label>
            CPF (somente números)
            <input
              value={cpf}
              onChange={(e) => setCpf(somenteDigitos(e.target.value).slice(0, 11))}
              inputMode="numeric"
              pattern="\d{11}"
              title="Informe os 11 dígitos do CPF"
              disabled={edicao}
              required
            />
          </label>
          <label>
            Data de nascimento
            <input
              type="date"
              value={dataNascimento}
              max={hojeISO()}
              onChange={(e) => setDataNascimento(e.target.value)}
              required
            />
          </label>
        </div>

        <label>
          Parentesco com o titular
          <input
            value={parentesco}
            onChange={(e) => setParentesco(e.target.value)}
            list="as-parentescos"
            placeholder="Ex.: Filho(a), Cônjuge"
            required
          />
          <datalist id="as-parentescos">
            <option value="Filho(a)" />
            <option value="Cônjuge" />
            <option value="Enteado(a)" />
            <option value="Neto(a)" />
            <option value="Irmão(ã)" />
          </datalist>
        </label>

        {acimaDoLimite && (
          <Aviso
            tipo="info"
            mensagem={`Com ${idade} anos, a pessoa não pode ser dependente (limite: menos de ${IDADE_LIMITE_DEPENDENTE} anos). Ela pode solicitar admissão própria como associado.`}
          />
        )}

        <div className="as-form-actions">
          <button type="button" className="as-btn as-btn-secundario" onClick={onFechar}>
            Cancelar
          </button>
          <button type="submit" className="as-btn as-btn-primario" disabled={salvando}>
            {salvando ? 'Salvando…' : edicao ? 'Salvar' : 'Cadastrar'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
