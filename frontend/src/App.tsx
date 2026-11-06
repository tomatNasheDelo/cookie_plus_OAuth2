import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'

const apiBase = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')
const api = (path: string) => `${apiBase}${path}`
type User = { authenticated: boolean; name: string | null; email: string | null }
type Csrf = { token: string; headerName: string }
type TokenStatus = { note: string; accessTokenExpiresAt: string | null; refreshTokenPresent: boolean }

export default function App() {
  const [user, setUser] = useState<User | null>(null)
  const [csrf, setCsrf] = useState<Csrf | null>(null)
  const [tokenStatus, setTokenStatus] = useState<TokenStatus | null>(null)
  const [actionMessage, setActionMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const loadMe = useCallback(async () => {
    const response = await fetch(api('/api/me'), { credentials: 'include' })
    if (!response.ok) throw new Error(`Проверка сессии: HTTP ${response.status}`)
    setUser(await response.json() as User)
  }, [])

  const loadCsrf = useCallback(async () => {
    const response = await fetch(api('/api/csrf'), { credentials: 'include' })
    if (!response.ok) throw new Error(`Получение CSRF token: HTTP ${response.status}`)
    setCsrf(await response.json() as Csrf)
  }, [])

  useEffect(() => {
    void Promise.all([loadMe(), loadCsrf()]).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : 'Не удалось подключиться'))
  }, [loadMe, loadCsrf])

  async function run(action: () => Promise<void>) {
    setBusy(true); setError(''); setActionMessage('')
    try { await action() } catch (reason) { setError(reason instanceof Error ? reason.message : 'Запрос не выполнен') }
    finally { setBusy(false) }
  }

  async function post(path: string) {
    if (!csrf) throw new Error('Сначала получи CSRF token')
    const response = await fetch(api(path), {
      method: 'POST', credentials: 'include',
      headers: { [csrf.headerName]: csrf.token },
    })
    if (!response.ok) throw new Error(`POST ${path}: HTTP ${response.status} (возможна CSRF/CORS ошибка)`)
    return response
  }

  function startLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    window.location.assign(api('/oauth2/authorization/keycloak'))
  }

  return <main className="shell">
    <header><p className="eyebrow">BFF · OPENID CONNECT · CSRF</p><h1>Cookie <span>+</span><br/>OAuth2</h1><p className="lead">Войти через OIDC provider, оставить OAuth tokens на сервере и исследовать session cookie, refresh и CSRF.</p></header>
    <section className="panel login">
      <div><small>OIDC LOGIN</small><h2>{user?.authenticated ? `Привет, ${user.name ?? 'пользователь'}!` : 'Вход через Keycloak'}</h2><p>{user?.authenticated ? user.email : 'Backend перенаправит на страницу входа identity provider.'}</p></div>
      {!user?.authenticated && <form onSubmit={startLogin}><button>Войти через OpenID Connect ↗</button></form>}
      {user?.authenticated && <button className="danger" disabled={busy} onClick={() => void run(async () => { await post('/api/logout'); setUser({ authenticated: false, name: null, email: null }); setActionMessage('Локальная backend session завершена.') })}>Выйти</button>}
    </section>
    <div className="grid">
      <section className="panel"><small>01 / SESSION COOKIE</small><h2>Состояние входа</h2><p>Браузер хранит только HttpOnly cookie LABSESSION. OAuth access/refresh tokens остаются на backend. Cookie нельзя прочитать через document.cookie.</p><button className="secondary" disabled={busy} onClick={() => void run(loadMe)}>Обновить статус</button><pre>{JSON.stringify(user, null, 2)}</pre><p className="hint">DevTools → Application/Storage → Cookies покажет LABSESSION и атрибуты.</p></section>
      <section className="panel"><small>02 / REFRESH TOKEN</small><h2>Обновление на backend</h2><p>Spring AuthorizedClientManager обновляет access token после истечения, если OIDC provider выдал refresh token. Этот endpoint не возвращает сами токены.</p><button className="secondary" disabled={busy || !user?.authenticated} onClick={() => void run(async () => { const response = await fetch(api('/api/oauth/refresh-check'), { credentials: 'include' }); if (!response.ok) throw new Error(`HTTP ${response.status}`); setTokenStatus(await response.json() as TokenStatus) })}>Проверить / обновить access token</button><pre>{JSON.stringify(tokenStatus, null, 2)}</pre><p className="hint">Для наблюдения автоматического refresh выставь у realm короткий access-token lifespan и подожди его истечения.</p></section>
    </div>
    <section className="panel csrf"><small>03 / CSRF</small><h2>Защищённое изменение состояния</h2><p>GET /api/csrf выдаёт CSRF token. React передаёт token в X-XSRF-TOKEN при POST; Spring Security отклонит POST без корректного токена.</p><button className="secondary" disabled={busy || !user?.authenticated} onClick={() => void run(async () => { await post('/api/demo/change-state'); setActionMessage('POST принят: CSRF token проверен.') })}>Выполнить защищённый POST</button><p className="hint">Для эксперимента удали csrf state в DevTools и попробуй отправить POST заново — сервер должен отказать.</p></section>
    {(actionMessage || error) && <p className={error ? 'notice error' : 'notice'} role={error ? 'alert' : 'status'}>{error || actionMessage}</p>}
    <section className="concepts"><h2>Что здесь чему соответствует?</h2><p><b>OIDC</b> — протокол входа пользователя поверх OAuth 2.0. <b>OAuth2 access token</b> — credential для API, а не cookie. <b>Refresh token</b> используется только backend для получения нового access token. <b>CSRF token</b> защищает cookie-authenticated POST от подделки сторонним сайтом.</p><p>Подробные команды и конфигурация Cloudflare/Render — в корневом <code>start.txt</code>.</p></section>
    <footer>OAuth tokens не отправляются в React · Учебный стенд</footer>
  </main>
}
