var api = require('../../api/index')
var STORAGE_KEYS = require('../../config/index').STORAGE_KEYS

Page({
  data: {
    phone: '13800000001',
    password: '123456',
    loading: false,
    wxLoading: false,
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

    api.login({ phone: phone, password: password })
      .then(function (user) {
        this.saveLoginAndGo(user)
      }.bind(this))
      .catch(function (error) {
        this.setData({
          error: error.message || '登录失败',
          loading: false,
        })
      }.bind(this))
  },

  onWxLogin: function () {
    if (this.data.wxLoading) return

    this.setData({
      wxLoading: true,
      error: '',
    })

    wx.login({
      success: function (res) {
        if (!res.code) {
          this.setData({
            wxLoading: false,
            error: '微信登录失败，未获取到 code',
          })
          return
        }

        api.wxLogin({ code: res.code })
          .then(function (user) {
            this.saveLoginAndGo(user)
          }.bind(this))
          .catch(function (error) {
            this.setData({
              wxLoading: false,
              error: error.message || '微信登录失败',
            })
          }.bind(this))
      }.bind(this),
      fail: function () {
        this.setData({
          wxLoading: false,
          error: '微信登录失败，请稍后重试',
        })
      }.bind(this),
    })
  },

  saveLoginAndGo: function (user) {
    wx.setStorageSync(STORAGE_KEYS.token, user.token)
    wx.setStorageSync(STORAGE_KEYS.user, user)
    this.setData({
      loading: false,
      wxLoading: false,
    })
    this.goAfterLogin()
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
