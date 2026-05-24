var login = require('../../api/index').login
var STORAGE_KEYS = require('../../config/index').STORAGE_KEYS

Page({
  data: {
    phone: '',
    password: '123456',
    loading: false,
    error: '',
  },

  onLoad: function () {
    var token = wx.getStorageSync(STORAGE_KEYS.token)
    if (token) {
      this.goAfterLogin()
    }
  },

  onPhoneInput: function (event) {
    this.setData({
      phone: event.detail.value.trim(),
      error: '',
    })
  },

  onPasswordInput: function (event) {
    this.setData({
      password: event.detail.value,
      error: '',
    })
  },

  onSubmit: function () {
    var phone = this.data.phone
    var password = this.data.password
    var loading = this.data.loading

    if (loading) return

    if (!phone) {
      wx.showToast({
        title: '请输入手机号',
        icon: 'none',
      })
      this.setData({ error: '请输入手机号' })
      return
    }

    if (!password) {
      wx.showToast({
        title: '请输入密码',
        icon: 'none',
      })
      this.setData({ error: '请输入密码' })
      return
    }

    this.setData({ loading: true, error: '' })

    login({ phone: phone, password: password })
      .then(function (user) {
        wx.setStorageSync(STORAGE_KEYS.token, user.token)
        wx.setStorageSync(STORAGE_KEYS.user, user)
        this.goAfterLogin()
      }.bind(this))
      .catch(function (error) {
        this.setData({
          error: error.message || '登录失败',
          loading: false,
        })
      }.bind(this))
  },

  goAfterLogin: function () {
    var table = wx.getStorageSync(STORAGE_KEYS.customerTable)
    if (table && table.tableId) {
      wx.redirectTo({ url: '/pages/menu/menu' })
      return
    }
    wx.redirectTo({ url: '/pages/table/table' })
  },
})
