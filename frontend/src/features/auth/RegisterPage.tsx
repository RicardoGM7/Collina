import { useState, type FormEvent } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import fundoLoginRegister from '../../assets/fundoLoginRegister.png'
import logoAzul from '../../assets/LogoAzulCollina.png'
import './LoginPage.css'

export function RegisterPage() {
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (password && confirmPassword && password !== confirmPassword) {
      setErrorMessage('As senhas não coincidem.')
      return
    }
    setErrorMessage('')
    navigate('/login')
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
          <h1 className="login-title">Register</h1>
          <p className="login-subtitle">Registre sua conta</p>
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
              placeholder="Exp: Lucas Neto da Silva"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
            />
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
                placeholder="Digite a sua senha"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value)
                  if (errorMessage) setErrorMessage('')
                }}
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
              Repetir Senha
            </label>
            <div className="login-input-wrap">
              <input
                id="register-confirm-password"
                type={showConfirmPassword ? 'text' : 'password'}
                className="login-input login-input-password"
                placeholder="Digite a sua senha"
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value)
                  if (errorMessage) setErrorMessage('')
                }}
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

          {errorMessage && (
            <p
              style={{
                fontSize: '12px',
                fontWeight: 600,
                color: '#E11D48',
                paddingLeft: '4px',
              }}
            >
              {errorMessage}
            </p>
          )}

          <button type="submit" className="login-submit-btn">
            Registrar
          </button>
        </form>

        <div className="login-footer">
          <span>Já possui conta? </span>
          <Link to="/login" className="login-link">
            Login
          </Link>
        </div>
      </div>
    </div>
  )
}
