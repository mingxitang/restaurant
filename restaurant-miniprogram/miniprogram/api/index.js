var request = require('../utils/request').request

function login(data) {
  return request('/api/auth/login', {
    method: 'POST',
    data: data,
    skipAuth: true,
  })
}

function logout() {
  return request('/api/auth/logout', { method: 'POST' })
}

function listTables() {
  return request('/api/tables')
}

function getMenu() {
  return request('/api/customer/menu')
}

function getOrder(id) {
  return request('/api/customer/orders/' + id)
}

function getActiveOrderByTable(tableId) {
  return request('/api/customer/tables/' + tableId + '/active-order')
}

function placeOrder(data) {
  return request('/api/customer/orders', {
    method: 'POST',
    data: data,
  })
}

function payOrder(id, data) {
  return request('/api/customer/orders/' + id + '/pay', {
    method: 'POST',
    data: data,
  })
}

function remindOrder(id) {
  return request('/api/customer/orders/' + id + '/remind', { method: 'POST' })
}

function reviewOrder(id, data) {
  return request('/api/customer/orders/' + id + '/review', {
    method: 'POST',
    data: data,
  })
}

function callWaiter(tableId, userId) {
  return request('/api/customer/call-waiter', {
    method: 'POST',
    data: {
      tableId: tableId,
      userId: userId,
      remark: '顾客呼叫服务员',
    },
  })
}

module.exports = {
  login: login,
  logout: logout,
  listTables: listTables,
  getMenu: getMenu,
  getOrder: getOrder,
  getActiveOrderByTable: getActiveOrderByTable,
  placeOrder: placeOrder,
  payOrder: payOrder,
  remindOrder: remindOrder,
  reviewOrder: reviewOrder,
  callWaiter: callWaiter,
}
