import http from './http'

export const login = (data) => http.post('/auth/login', data)
export const dashboard = () => http.get('/reports/dashboard')
export const monthlyRevenue = () => http.get('/reports/monthly-revenue')
export const hotDishes = () => http.get('/reports/hot-dishes')
export const refundReasons = () => http.get('/reports/refund-reasons')

export const listUsers = (params) => http.get('/users', { params })
export const createUser = (data) => http.post('/users', data)
export const updateUser = (id, data) => http.put(`/users/${id}`, data)

export const listCategories = () => http.get('/categories')
export const createCategory = (data) => http.post('/categories', data)
export const updateCategory = (id, data) => http.put(`/categories/${id}`, data)
export const deleteCategory = (id) => http.delete(`/categories/${id}`)

export const listDishes = (params) => http.get('/dishes', { params })
export const createDish = (data) => http.post('/dishes', data)
export const updateDish = (id, data) => http.put(`/dishes/${id}`, data)
export const deleteDish = (id) => http.delete(`/dishes/${id}`)
export const lowStockDishes = (params) => http.get('/dishes/low-stock', { params })

export const listTables = (params) => http.get('/tables', { params })
export const createTable = (data) => http.post('/tables', data)
export const updateTable = (id, data) => http.put(`/tables/${id}`, data)
export const updateTableStatus = (id, status) => http.put(`/tables/${id}/status`, { status })
export const deleteTable = (id) => http.delete(`/tables/${id}`)

export const listOrders = (params) => http.get('/orders', { params })
export const getOrder = (id) => http.get(`/orders/${id}`)
export const createOrder = (data) => http.post('/orders', data)
export const payOrder = (id, data) => http.put(`/orders/${id}/pay`, data)
export const cancelOrder = (id) => http.put(`/orders/${id}/cancel`)
export const updateOrderStatus = (id, status) => http.put(`/orders/${id}/status`, { status })

export const listRefunds = () => http.get('/refunds')
export const createRefund = (data) => http.post('/refunds', data)

export const listReviews = () => http.get('/reviews')
export const createReview = (data) => http.post('/reviews', data)
