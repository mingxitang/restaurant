var config = require('../config/index')
var API_BASE_URL = config.API_BASE_URL
var STORAGE_KEYS = config.STORAGE_KEYS

function buildUrl(path) {
  if (path.indexOf('http://') === 0 || path.indexOf('https://') === 0) {
    return path
  }

  return API_BASE_URL + (path.indexOf('/') === 0 ? path : '/' + path)
}

function redirectToLogin() {
  wx.removeStorageSync(STORAGE_KEYS.token)
  wx.removeStorageSync(STORAGE_KEYS.user)
  wx.redirectTo({ url: '/pages/login/login' })
}

function request(path, options) {
  options = options || {}
  var token = wx.getStorageSync(STORAGE_KEYS.token)
  var header = Object.assign({
    'content-type': 'application/json',
  }, options.header || {})

  if (token && !options.skipAuth) {
    header.Authorization = 'Bearer ' + token
  }

  return new Promise(function (resolve, reject) {
    wx.request({
      url: buildUrl(path),
      method: options.method || 'GET',
      data: options.data,
      header: header,
      success: function (response) {
        if (response.statusCode === 401 || response.statusCode === 403) {
          redirectToLogin()
          reject(new Error('登录已过期，请重新登录'))
          return
        }

        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error('请求失败：' + response.statusCode))
          return
        }

        var payload = response.data
        if (payload && payload.code && payload.code !== 200) {
          reject(new Error(payload.message || '请求失败'))
          return
        }

        resolve(payload ? payload.data : undefined)
      },
      fail: function (error) {
        var message = error && error.errMsg
          ? error.errMsg
          : '网络请求失败，请检查后端服务是否启动'
        reject(new Error(message))
      },
    })
  })
}

module.exports = {
  request: request,
}
