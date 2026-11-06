import { useEffect, useRef } from 'react'

const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID

export default function GoogleSignInButton({ onCredential, disabled }) {
  const container = useRef(null)
  const callback = useRef(onCredential)
  callback.current = onCredential

  useEffect(() => {
    if (!CLIENT_ID) return
    let cancelled = false
    let timer
    const render = () => {
      if (cancelled || !container.current) return
      if (!window.google?.accounts?.id) { timer = setTimeout(render, 200); return }
      window.google.accounts.id.initialize({
        client_id: CLIENT_ID,
        callback: response => callback.current(response.credential),
      })
      window.google.accounts.id.renderButton(container.current, {
        theme: 'outline', size: 'large', text: 'signin_with', shape: 'rectangular', width: 360,
      })
    }
    render()
    return () => { cancelled = true; clearTimeout(timer) }
  }, [])

  if (!CLIENT_ID) return null
  return (
    <div className={disabled ? 'pointer-events-none opacity-50' : ''}>
      <div className="relative py-2 text-center text-sm text-gray-400">
        <span className="bg-white px-3 dark:bg-gray-900">hoặc</span>
      </div>
      <div ref={container} className="flex justify-center" />
    </div>
  )
}
