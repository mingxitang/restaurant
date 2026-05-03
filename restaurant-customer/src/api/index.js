import http from './http'

export const login = (data) => http.post('/auth/login', data)

export const getMenu = () => http.get('/customer/menu')
export const getOrder = (id) => http.get(`/customer/orders/${id}`)
export const placeOrder = (data) => http.post('/customer/orders', data)
export const payOrder = (id, data) => http.post(`/customer/orders/${id}/pay`, data)
export const reviewOrder = (id, data) => http.post(`/customer/orders/${id}/review`, data)
export const callWaiter = (tableId) => http.post('/customer/call-waiter', { tableId })

export const listTables = () => http.get('/tables')
