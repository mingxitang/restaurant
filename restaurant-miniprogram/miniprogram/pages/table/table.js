var api = require('../../api/index')
var STORAGE_KEYS = require('../../config/index').STORAGE_KEYS

function parseTableParam(options) {
  if (!options) return ''
  if (options.tableId || options.tableNumber || options.table) {
    return options.tableId || options.tableNumber || options.table
  }
  if (!options.scene) return ''

  var scene = decodeURIComponent(options.scene)
  var pairs = scene.split('&')
  for (var i = 0; i < pairs.length; i += 1) {
    var pair = pairs[i].split('=')
    if (pair[0] === 'tableId' || pair[0] === 'tableNumber' || pair[0] === 'table') {
      return pair[1]
    }
  }

  return scene
}

Page({
  data: {
    tables: [],
    manualNumber: '',
    pendingTableValue: '',
    loading: false,
    selecting: false,
    error: '',
  },

  onLoad: function (options) {
    var pending = parseTableParam(options)
    this.setData({
      pendingTableValue: pending ? String(pending) : '',
    })
    this.loadTables()
  },

  onPullDownRefresh: function () {
    this.loadTables(function () {
      wx.stopPullDownRefresh()
    })
  },

  loadTables: function (done) {
    this.setData({
      loading: true,
      error: '',
    })

    api.listTables()
      .then(function (tables) {
        this.setData({
          tables: Array.isArray(tables) ? tables : [],
          loading: false,
        })
        this.tryAutoSelectFromQuery()
        if (typeof done === 'function') {
          done()
        }
      }.bind(this))
      .catch(function (error) {
        this.setData({
          error: error.message || '加载桌台失败',
          loading: false,
        })
        if (typeof done === 'function') {
          done()
        }
      }.bind(this))
  },

  onManualInput: function (event) {
    this.setData({
      manualNumber: event.detail.value.trim(),
      error: '',
    })
  },

  onSelectTable: function (event) {
    var index = event.currentTarget.dataset.index
    var table = this.data.tables[index]

    if (!table) return

    this.saveTableAndGo(table)
  },

  onManualConfirm: function () {
    var value = this.data.manualNumber
    var table = this.findTable(value)

    if (!value) {
      this.setData({ error: '请输入桌号' })
      return
    }

    if (!table) {
      this.setData({ error: '未找到该桌台，请检查桌号' })
      return
    }

    this.saveTableAndGo(table)
  },

  findTable: function (value) {
    var tables = this.data.tables
    var text = String(value)

    for (var i = 0; i < tables.length; i += 1) {
      var table = tables[i]
      if (
        String(table.tableId) === text ||
        String(table.tableNumber) === text ||
        String(table.tableName) === text
      ) {
        return table
      }
    }

    return null
  },

  saveTableAndGo: function (table) {
    var previous = wx.getStorageSync(STORAGE_KEYS.customerTable)
    var selected = {
      tableId: table.tableId,
      tableNumber: table.tableName || table.tableNumber,
    }

    if (this.data.selecting) return

    this.setData({
      selecting: true,
      error: '',
    })

    wx.setStorageSync(STORAGE_KEYS.customerTable, selected)
    wx.removeStorageSync(STORAGE_KEYS.customerCart)

    api.getActiveOrderByTable(selected.tableId)
      .then(function (order) {
        if (order && order.orderId) {
          wx.setStorageSync(STORAGE_KEYS.currentOrder, order)
        } else if (!previous || String(previous.tableId) !== String(selected.tableId)) {
          wx.removeStorageSync(STORAGE_KEYS.currentOrder)
        }
        this.setData({ selecting: false })
        wx.navigateTo({ url: '/pages/menu/menu' })
      }.bind(this))
      .catch(function (error) {
        this.setData({
          selecting: false,
          error: error.message || '获取桌台订单失败',
        })
      }.bind(this))
  },

  tryAutoSelectFromQuery: function () {
    var value = this.data.pendingTableValue
    if (!value) return

    var table = this.findTable(value)
    this.setData({
      pendingTableValue: '',
      manualNumber: value,
    })

    if (table) {
      this.saveTableAndGo(table)
    } else {
      this.setData({
        error: '扫码桌号未找到，请手动选择桌台',
      })
    }
  },

  onLogout: function () {
    api.logout()
      .then(function () {
        wx.clearStorageSync()
        wx.redirectTo({ url: '/pages/login/login' })
      })
      .catch(function () {
        wx.clearStorageSync()
        wx.redirectTo({ url: '/pages/login/login' })
      })
  },
})
