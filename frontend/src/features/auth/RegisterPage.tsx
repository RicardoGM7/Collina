import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { Link } from 'react-router-dom'
import fundoLoginRegister from '../../assets/fundoLoginRegister.png'
import logoAzul from '../../assets/LogoAzulCollina.png'
import { firebaseConfigurado } from '../../config/firebase'
import { useAuth } from '../../context/AuthContext'
import { associadoService } from '../../services/associadoService'
import {
  CATEGORIA_DO_PLANO,
  nomePlano,
  type CategoriaAssociado,
  type Plano,
  type TipoPlano,
} from '../../types/associado'
import {
  formatarCpf,
  formatarMoeda,
  mensagemErroApi,
  validarCpf,
} from '../associados/associadoUtils'
import { mensagemDeErro } from './authErrors'
import './LoginPage.css'

export function RegisterPage() {
  const { cadastrar } = useAuth()
  const [submitting, setSubmitting] = useState(false)
  const [fullName, setFullName] = useState('')
  const [cpf, setCpf] = useState('')
  const [dataNascimento, setDataNascimento] = useState('')
  const [categoria, setCategoria] =
    useState<CategoriaAssociado>('CONTRIBUINTE')
  const [tipoPlano, setTipoPlano] = useState<TipoPlano>('BASICO')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const [planos, setPlanos] = useState<Plano[]>([])
  const [carregandoPlanos, setCarregandoPlanos] = useState(true)

  useEffect(() => {
    let ativo = true
    associadoService
      .listarPlanos()
      .then((lista) => {
        if (!ativo) return
        setPlanos(lista)
        const primeiro = lista.find(
          (p) => CATEGORIA_DO_PLANO[p.tipo] === 'CONTRIBUINTE',
        )
        if (primeiro) setTipoPlano(primeiro.tipo)
      })
      .catch((e) => {
        if (ativo) {
          setErrorMessage(
            mensagemErroApi(e, 'Não foi possível carregar os planos do clube.'),
          )
        }
      })
      .finally(() => {
        if (ativo) setCarregandoPlanos(false)
      })
    return () => {
      ativo = false
    }
  }, [])

  const planosDaCategoria = useMemo(
    () => planos.filter((p) => CATEGORIA_DO_PLANO[p.tipo] === categoria),
    [planos, categoria],
  )

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!fullName.trim() || !email.trim() || !password) {
      setErrorMessage('Preencha nome, e-mail e senha.')
      return
    }
    const cleanCpf = cpf.replace(/\D/g, '')
    if (cleanCpf.length !== 11 || !validarCpf(cleanCpf)) {
      setErrorMessage('Informe um CPF válido com 11 dígitos.')
      return
    }
    if (!dataNascimento) {
      setErrorMessage('Informe a data de nascimento.')
      return
    }
    if (password.length < 6) {
      setErrorMessage('A senha deve ter pelo menos 6 caracteres.')
      return
    }
    if (password !== confirmPassword) {
      setErrorMessage('As senhas não coincidem.')
      return
    }

    setErrorMessage('')
    setSubmitting(true)
    try {
      await cadastrar(fullName, email, password, async () => {
        await associadoService.cadastrarMeuPerfil({
          nome: fullName.trim(),
          cpf: cleanCpf,
          dataNascimento,
          categoria,
          tipoPlano,
        })
      })
    } catch (erro) {
      const msgApi = mensagemErroApi(erro, '')
      setErrorMessage(msgApi || mensagemDeErro(erro))
      setSubmitting(false)
    }
  }

  return (
    <div
      className="login-page"
      style={{ backgroundImage: `url(${fundoLoginRegister})` }}
    >
      <div className="login-card">
        <div className="login-brand">
          <img
            src={logoAzul}
            alt="Collina Sports Club"
            className="login-logo"
          />
          <h1 className="login-title">Cadastro de Associado</h1>
          <p className="login-subtitle">Crie sua conta de associado titular</p>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          <div className="login-field">
            <label htmlFor="register-name" className="login-label">
              Nome Completo
            </label>
            <input
              id="register-name"
              type="text"
              className="login-input"
              placeholder="Ex: Maria Souza"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
            />
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: '10px',
            }}
          >
            <div className="login-field">
              <label htmlFor="register-cpf" className="login-label">
                CPF
              </label>
              <input
                id="register-cpf"
                type="text"
                maxLength={14}
                className="login-input"
                placeholder="000.000.000-00"
                value={formatarCpf(cpf)}
                onChange={(e) => setCpf(e.target.value)}
              />
            </div>

            <div className="login-field">
              <label htmlFor="register-nasc" className="login-label">
                Nascimento
              </label>
              <input
                id="register-nasc"
                type="date"
                className="login-input"
                value={dataNascimento}
                onChange={(e) => setDataNascimento(e.target.value)}
              />
            </div>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: '10px',
            }}
          >
            <div className="login-field">
              <label htmlFor="register-cat" className="login-label">
                Categoria
              </label>
              <select
                id="register-cat"
                className="login-input"
                value={categoria}
                onChange={(e) => {
                  const novaCat = e.target.value as CategoriaAssociado
                  setCategoria(novaCat)
                  const primeiro = planos.find(
                    (p) => CATEGORIA_DO_PLANO[p.tipo] === novaCat,
                  )
                  if (primeiro) setTipoPlano(primeiro.tipo)
                  else
                    setTipoPlano(
                      novaCat === 'PROPRIETARIO' ? 'PROPRIETARIO' : 'BASICO',
                    )
                }}
              >
                <option value="CONTRIBUINTE">Contribuinte</option>
                <option value="PROPRIETARIO">Proprietário</option>
              </select>
            </div>

            <div className="login-field">
              <label htmlFor="register-plano" className="login-label">
                Plano
              </label>
              <select
                id="register-plano"
                className="login-input"
                value={tipoPlano}
                disabled={carregandoPlanos || planosDaCategoria.length === 0}
                onChange={(e) => setTipoPlano(e.target.value as TipoPlano)}
              >
                {carregandoPlanos ? (
                  <option>Carregando...</option>
                ) : (
                  planosDaCategoria.map((p) => (
                    <option key={p.id} value={p.tipo}>
                      {nomePlano(p).replace('Plano ', '')} (
                      {formatarMoeda(p.valorPeriodico)})
                    </option>
                  ))
                )}
              </select>
            </div>
          </div>

          <div className="login-field">
            <label htmlFor="register-email" className="login-label">
              Email
            </label>
            <input
              id="register-email"
              type="email"
              className="login-input"
              placeholder="Digite seu email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <div className="login-field">
            <label htmlFor="register-password" className="login-label">
              Senha
            </label>
            <div className="login-input-wrap">
              <input
                id="register-password"
                type={showPassword ? 'text' : 'password'}
                className="login-input login-input-password"
                placeholder="Mínimo 6 caracteres"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <button
                type="button"
                className="login-eye-btn"
                onClick={() => setShowPassword((prev) => !prev)}
                aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
              >
                {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
              </button>
            </div>
          </div>

          <div className="login-field">
            <label htmlFor="register-confirm-password" className="login-label">
              Confirmar Senha
            </label>
            <div className="login-input-wrap">
              <input
                id="register-confirm-password"
                type={showConfirmPassword ? 'text' : 'password'}
                className="login-input login-input-password"
                placeholder="Repita sua senha"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
              <button
                type="button"
                className="login-eye-btn"
                onClick={() => setShowConfirmPassword((prev) => !prev)}
                aria-label={
                  showConfirmPassword ? 'Ocultar senha' : 'Mostrar senha'
                }
              >
                {showConfirmPassword ? <EyeOff size={17} /> : <Eye size={17} />}
              </button>
            </div>
          </div>

          {!firebaseConfigurado && (
            <p className="login-message login-message-warning">
              Firebase não configurado: preencha frontend/.env.local (veja
              .env.example).
            </p>
          )}
          {errorMessage && (
            <p className="login-message login-message-error" role="alert">
              {errorMessage}
            </p>
          )}

          <button
            type="submit"
            className="login-submit-btn"
            disabled={submitting || carregandoPlanos}
          >
            {submitting ? 'Cadastrando...' : 'Criar conta'}
          </button>
        </form>

        <div className="login-footer">
          <span>Já possui conta? </span>
          <Link to="/login" className="login-link">
            Entrar
          </Link>
        </div>
      </div>
    </div>
  )
}
