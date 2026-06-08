import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && payload.code && payload.code !== 200) {
      return Promise.reject(new Error(payload.message || '请求失败'))
    }
    return payload.data
  },
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      const message = error.response.status === 401
        ? '登录已过期，请重新登录'
        : '权限不足，请使用有权限的账号登录'
      if (window.confirm(message)) {
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('user')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error.response?.data?.message ? new Error(error.response.data.message) : error)
  }
)

export default http
