import { useState, type FormEvent } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import fundoLoginRegister from '../../assets/fundoLoginRegister.png'
import logoAzul from '../../assets/LogoAzulCollina.png'
import './LoginPage.css'

export function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    navigate('/admin')
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
            <button type="button" className="login-link">
              Esqueceu a senha?
            </button>
          </div>

          <button type="submit" className="login-submit-btn">
            Entrar
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
