//var API_BASE_URL = 'http://localhost:8080'
var API_BASE_URL = 'http://192.168.0.101:8080'

var STORAGE_KEYS = {
  token: 'token',
  user: 'user',
  customerTable: 'customerTable',
  customerCart: 'customerCart',
  currentOrder: 'currentOrder',
}

module.exports = {
  API_BASE_URL: API_BASE_URL,
  STORAGE_KEYS: STORAGE_KEYS,
}
