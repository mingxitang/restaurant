var api = require('../../api/index')
var STORAGE_KEYS = require('../../config/index').STORAGE_KEYS

function money(value) {
  var number = Number(value || 0)
  return number.toFixed(2)
}

function statusText(status) {
  var map = {
    PENDING: '待支付',
    PAID: '已支付',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
  }
  return map[status] || status || '未知'
}

function normalizeOrder(order) {
  if (!order) return null
  order.statusText = statusText(order.status)
  order.totalAmountText = money(order.totalAmount)
  order.canPay = order.status === 'PENDING'
  order.canReview = order.status === 'PAID' || order.status === 'COMPLETED'
  order.reviewedText = order.reviewed ? '已评价' : '待评价'
  return order
}

Page({
  data: {
    mode: 'pay',
    title: '支付',
    order: null,
    payMethod: 'WECHAT',
    rating: 5,
    ratingOptions: [1, 2, 3, 4, 5],
    content: '',
    loading: false,
    paying: false,
    submittingReview: false,
    error: '',
    message: '',
  },

  onLoad: function (options) {
    var isReview = options && options.review === '1'
    this.setData({
      mode: isReview ? 'review' : 'pay',
      title: isReview ? '评价订单' : '支付订单',
    })
    this.loadOrder()
  },

  onShow: function () {
    if (this.data.order && this.data.order.orderId) {
      this.loadOrder(true)
    }
  },

  loadOrder: function (silent) {
    var savedOrder = wx.getStorageSync(STORAGE_KEYS.currentOrder)
    var orderId = savedOrder && savedOrder.orderId

    if (!orderId) {
      this.setData({
        order: null,
        loading: false,
      })
      return
    }

    if (!silent) {
      this.setData({
        loading: true,
        error: '',
      })
    }

    api.getOrder(orderId)
      .then(function (order) {
        var normalized = normalizeOrder(order)
        wx.setStorageSync(STORAGE_KEYS.currentOrder, normalized)
        this.setData({
          order: normalized,
          loading: false,
          error: '',
        })
      }.bind(this))
      .catch(function (error) {
        this.setData({
          order: normalizeOrder(savedOrder),
          loading: false,
          error: error.message || '加载订单失败',
        })
      }.bind(this))
  },

  onPayMethodChange: function (event) {
    this.setData({
      payMethod: event.detail.value,
    })
  },

  onPay: function () {
    var order = this.data.order

    if (!order || !order.orderId || this.data.paying) return

    if (!order.canPay) {
      wx.showToast({
        title: '该订单无需支付',
        icon: 'none',
      })
      return
    }

    this.setData({
      paying: true,
      error: '',
      message: '',
    })

    api.payOrder(order.orderId, {
      payMethod: this.data.payMethod,
    })
      .then(function () {
        wx.showToast({
          title: '支付成功',
          icon: 'success',
        })
        this.setData({
          paying: false,
          message: '支付成功',
        })
        this.loadOrder(true)
        setTimeout(function () {
          wx.navigateBack()
        }, 600)
      }.bind(this))
      .catch(function (error) {
        this.setData({
          paying: false,
          error: error.message || '支付失败',
        })
      }.bind(this))
  },

  onRatingTap: function (event) {
    this.setData({
      rating: Number(event.currentTarget.dataset.value || 5),
      error: '',
    })
  },

  onContentInput: function (event) {
    this.setData({
      content: event.detail.value,
      error: '',
    })
  },

  onSubmitReview: function () {
    var order = this.data.order

    if (!order || !order.orderId || this.data.submittingReview) return

    if (!order.canReview) {
      wx.showToast({
        title: '该订单暂不能评价',
        icon: 'none',
      })
      return
    }

    this.setData({
      submittingReview: true,
      error: '',
      message: '',
    })

    api.reviewOrder(order.orderId, {
      rating: this.data.rating,
      comment: this.data.content,
    })
      .then(function () {
        wx.showToast({
          title: '评价成功',
          icon: 'success',
        })
        order.reviewed = true
        wx.setStorageSync(STORAGE_KEYS.currentOrder, order)
        this.setData({
          order: normalizeOrder(order),
          submittingReview: false,
          message: '感谢您的评价',
        })
        setTimeout(function () {
          wx.navigateBack()
        }, 600)
      }.bind(this))
      .catch(function (error) {
        this.setData({
          submittingReview: false,
          error: error.message || '评价失败',
        })
      }.bind(this))
  },

  onBackOrder: function () {
    wx.navigateBack()
  },
})
