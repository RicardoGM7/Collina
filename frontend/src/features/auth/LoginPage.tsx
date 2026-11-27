import { useState, type FormEvent } from 'react'
import { FirebaseError } from 'firebase/app'
import { Eye, EyeOff } from 'lucide-react'
import { Link } from 'react-router-dom'
import fundoLoginRegister from '../../assets/fundoLoginRegister.png'
import logoAzul from '../../assets/LogoAzulCollina.png'
import { firebaseConfigurado } from '../../config/firebase'
import { useAuth } from '../../context/AuthContext'
import { mensagemDeErro, mensagemDeErroLogin } from './authErrors'
import './LoginPage.css'

const MENSAGEM_RECUPERACAO_NEUTRA =
  'Se o e-mail estiver cadastrado, você receberá um link para redefinir a senha.'

export function LoginPage() {
  const { login, recuperarSenha } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [infoMessage, setInfoMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // Após o login bem-sucedido, o PublicOnlyRoute redireciona para a tela inicial do perfil
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorMessage('')
    setInfoMessage('')

    if (!email.trim() || !password) {
      setErrorMessage('Informe o e-mail e a senha.')
      return
    }

    setSubmitting(true)
    try {
      await login(email, password)
    } catch (erro) {
      setErrorMessage(mensagemDeErroLogin(erro))
      setSubmitting(false)
    }
  }

  async function handleForgotPassword() {
    setErrorMessage('')
    setInfoMessage('')

    if (!email.trim()) {
      setErrorMessage('Digite o seu e-mail acima para receber o link de redefinição.')
      return
    }

    try {
      await recuperarSenha(email)
      setInfoMessage(MENSAGEM_RECUPERACAO_NEUTRA)
    } catch (erro) {
      // A resposta deve ser idêntica exista ou não conta com o e-mail informado
      if (erro instanceof FirebaseError && erro.code === 'auth/user-not-found') {
        setInfoMessage(MENSAGEM_RECUPERACAO_NEUTRA)
        return
      }
      setErrorMessage(mensagemDeErro(erro))
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
          <h1 className="login-title">Login</h1>
          <p className="login-subtitle">Entre para acessar a sua conta</p>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          <div className="login-field">
            <label htmlFor="login-email" className="login-label">
              Email
            </label>
            <input
              id="login-email"
              type="email"
              className="login-input"
              placeholder="exemplo@gmail.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <div className="login-field">
            <label htmlFor="login-password" className="login-label">
              Senha
            </label>
            <div className="login-input-wrap">
              <input
                id="login-password"
                type={showPassword ? 'text' : 'password'}
                className="login-input login-input-password"
                placeholder="Digite a sua senha"
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

          <div className="login-forgot-row">
            <button
              type="button"
              className="login-link"
              onClick={() => void handleForgotPassword()}
            >
              Esqueceu a senha?
            </button>
          </div>

          {!firebaseConfigurado && (
            <p className="login-message login-message-warning">
              Firebase não configurado: preencha frontend/.env.local (veja .env.example).
            </p>
          )}
          {errorMessage && (
            <p className="login-message login-message-error" role="alert">
              {errorMessage}
            </p>
          )}
          {infoMessage && (
            <p className="login-message login-message-info" role="status">
              {infoMessage}
            </p>
          )}

          <button type="submit" className="login-submit-btn" disabled={submitting}>
            {submitting ? 'Entrando...' : 'Entrar'}
          </button>
        </form>

        <div className="login-footer">
          <span>Não possui login? </span>
          <Link to="/register" className="login-link">
            Cadastre-se
          </Link>
        </div>
      </div>
    </div>
  )
}
