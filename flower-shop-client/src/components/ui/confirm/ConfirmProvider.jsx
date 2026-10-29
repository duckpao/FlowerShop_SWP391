import { createContext, useCallback, useContext, useRef, useState } from 'react'
import { Modal } from '../modal'
import Button from '../button/Button'

const ConfirmContext = createContext(null)

const toneClass = {
    danger: 'bg-error-500 hover:bg-error-600 text-white',
    primary: '',
}

/**
 * Hosts the app's confirm and notice dialogs. Call `useConfirm()` and await the result
 * instead of window.confirm / window.alert.
 */
export function ConfirmProvider({ children }) {
    const [dialog, setDialog] = useState(null)
    const resolver = useRef(null)

    const open = useCallback(options => new Promise(resolve => {
        resolver.current = resolve
        setDialog(options)
    }), [])

    const close = result => {
        resolver.current?.(result)
        resolver.current = null
        setDialog(null)
    }

    const confirm = useCallback(options => open({ ...options, kind: 'confirm' }), [open])
    const notify = useCallback(options => open({ ...options, kind: 'notice' }), [open])

    return <ConfirmContext.Provider value={{ confirm, notify }}>
        {children}
        <Modal isOpen={!!dialog} onClose={() => close(false)} showCloseButton={false} className="max-w-md p-6">
            {dialog && <div role="alertdialog" aria-labelledby="confirm-title" aria-describedby="confirm-message">
                <h3 id="confirm-title" className="text-lg font-semibold text-gray-900 dark:text-white">
                    {dialog.title || (dialog.kind === 'confirm' ? 'Xác nhận thao tác' : 'Thông báo')}
                </h3>
                <p id="confirm-message" className="mt-2 text-sm text-gray-600 dark:text-gray-400">{dialog.message}</p>
                <div className="mt-6 flex justify-end gap-3">
                    {dialog.kind === 'confirm' && <Button variant="outline" onClick={() => close(false)}>
                        {dialog.cancelLabel || 'Quay lại'}
                    </Button>}
                    <Button className={toneClass[dialog.tone] || ''} onClick={() => close(true)}>
                        {dialog.confirmLabel || (dialog.kind === 'confirm' ? 'Xác nhận' : 'Đã hiểu')}
                    </Button>
                </div>
            </div>}
        </Modal>
    </ConfirmContext.Provider>
}

export function useConfirm() {
    const ctx = useContext(ConfirmContext)
    if (!ctx) throw new Error('useConfirm must be used inside ConfirmProvider')
    return ctx
}
