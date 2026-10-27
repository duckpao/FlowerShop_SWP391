import { Client } from '@stomp/stompjs'
import { API_BASE } from '../apiBase'
import { authModel } from '../models/authModel'
import { authService } from './authService'

const socketUrl = () => {
    const origin = API_BASE
        ? API_BASE.replace(/^http/, 'ws')
        : `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}`
    return `${origin}/ws`
}

// destination -> { callbacks, subscription }. Đăng ký lại tự động sau mỗi lần nối lại.
const topics = new Map()
const stateListeners = new Set()
let client = null
let connected = false

const setConnected = value => {
    connected = value
    stateListeners.forEach(listener => listener(value))
}

function subscribeTopic(destination, entry) {
    if (!client || !connected || entry.subscription) return
    entry.subscription = client.subscribe(destination, frame => {
        let payload = frame.body
        try { payload = JSON.parse(frame.body) } catch { /* kênh read gửi chuỗi thường */ }
        entry.callbacks.forEach(callback => callback(payload))
    })
}

function ensureClient() {
    if (client) return
    client = new Client({
        brokerURL: socketUrl(),
        reconnectDelay: 3000,
        // Token đọc lại ở mỗi lần nối để dùng token mới sau khi làm mới phiên
        beforeConnect: () => { client.connectHeaders = { Authorization: `Bearer ${authModel.getToken() || ''}` } },
        onConnect: () => {
            setConnected(true)
            topics.forEach((entry, destination) => { entry.subscription = null; subscribeTopic(destination, entry) })
        },
        onWebSocketClose: () => {
            setConnected(false)
            topics.forEach(entry => { entry.subscription = null })
        },
        // Token hết hạn bị từ chối ở CONNECT: làm mới phiên, lần nối lại kế tiếp sẽ dùng token mới
        onStompError: () => { authService.me().catch(() => {}) },
    })
    client.activate()
}

export const chatSocket = {
    /** Nghe một kênh; trả về hàm hủy. Kết nối được mở ở lần nghe đầu tiên và đóng khi không còn ai nghe. */
    subscribe(destination, callback) {
        ensureClient()
        let entry = topics.get(destination)
        if (!entry) {
            entry = { callbacks: new Set(), subscription: null }
            topics.set(destination, entry)
        }
        entry.callbacks.add(callback)
        subscribeTopic(destination, entry)
        return () => {
            entry.callbacks.delete(callback)
            if (entry.callbacks.size > 0) return
            entry.subscription?.unsubscribe()
            topics.delete(destination)
            if (topics.size === 0 && client) {
                client.deactivate()
                client = null
                setConnected(false)
            }
        }
    },
    /** Theo dõi trạng thái kết nối để báo "đang nối lại". */
    onState(listener) {
        stateListeners.add(listener)
        listener(connected)
        return () => stateListeners.delete(listener)
    },
}
