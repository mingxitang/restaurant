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

function cookingText(status) {
  var map = {
    PENDING: '待制作',
    PREPARING: '制作中',
    READY: '待上菜',
    SERVED: '已上菜',
  }
  return map[status] || status || ''
}

function normalizeOrder(order) {
  if (!order) return null

  var details = Array.isArray(order.details) ? order.details : []
  for (var i = 0; i < details.length; i += 1) {
    details[i].unitPriceText = money(details[i].unitPrice)
    details[i].amountText = money(Number(details[i].unitPrice || 0) * Number(details[i].quantity || 0))
    details[i].cookingText = cookingText(details[i].status)
    details[i].hasRemark = Boolean(details[i].remark)
  }

  order.details = details
  order.statusText = statusText(order.status)
  order.totalAmountText = money(order.totalAmount)
  order.canPay = order.status === 'PENDING'
  order.canRemind = order.status === 'PENDING' || order.status === 'PAID'
  order.canReview = order.status === 'PAID' || order.status === 'COMPLETED'
  order.reviewButtonText = order.reviewed ? '查看评价' : '去评价'
  order.reminderText = order.reminderCount ? '已催单 ' + order.reminderCount + ' 次' : '菜品久等时可以提醒厨房'

  return order
}

Page({
  data: {
    table: null,
    tableNumber: '',
    subtitle: '查看当前订单状态',
    order: null,
    loading: false,
    reminding: false,
    message: '',
    error: '',
  },

  onLoad: function () {
    var table = wx.getStorageSync(STORAGE_KEYS.customerTable)
    this.setData({
      table: table || null,
      tableNumber: table ? table.tableNumber : '',
      subtitle: table ? '桌台：' + table.tableNumber : '查看当前订单状态',
    })
    this.loadOrder()
  },

  onShow: function () {
    if (this.data.order && this.data.order.orderId) {
      this.loadOrder(true)
    }
  },

  onPullDownRefresh: function () {
    this.loadOrder(false, function () {
      wx.stopPullDownRefresh()
    })
  },

  loadOrder: function (silent, done) {
    var savedOrder = wx.getStorageSync(STORAGE_KEYS.currentOrder)
    var orderId = savedOrder && savedOrder.orderId

    if (!orderId) {
      this.setData({
        order: null,
        loading: false,
      })
      if (typeof done === 'function') done()
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
        if (typeof done === 'function') done()
      }.bind(this))
      .catch(function (error) {
        var fallback = normalizeOrder(savedOrder)
        this.setData({
          order: fallback,
          loading: false,
          error: error.message || '加载订单失败',
        })
        if (typeof done === 'function') done()
      }.bind(this))
  },

  onRefresh: function () {
    this.loadOrder()
  },

  onRemind: function () {
    var order = this.data.order

    if (!order || !order.orderId || this.data.reminding) return

    this.setData({
      reminding: true,
      message: '',
      error: '',
    })

    api.remindOrder(order.orderId)
      .then(function () {
        this.setData({
          message: '已帮您催单，厨房看板会收到提醒。',
          reminding: false,
        })
        this.loadOrder(true)
      }.bind(this))
      .catch(function (error) {
        this.setData({
          message: '',
          error: error.message || '催单失败',
          reminding: false,
        })
      }.bind(this))
  },

  onGoMenu: function () {
    wx.navigateTo({ url: '/pages/menu/menu' })
  },

  onGoPay: function () {
    wx.navigateTo({ url: '/pages/pay/pay' })
  },

  onGoReview: function () {
    wx.navigateTo({ url: '/pages/pay/pay?review=1' })
  },
})
