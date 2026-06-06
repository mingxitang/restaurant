var api = require('../../api/index')
var config = require('../../config/index')
var STORAGE_KEYS = config.STORAGE_KEYS
var API_BASE_URL = config.API_BASE_URL

function money(value) {
  var number = Number(value || 0)
  return number.toFixed(2)
}

function normalizeImage(image) {
  if (!image) return ''
  if (image.indexOf('http://') === 0 || image.indexOf('https://') === 0) {
    return image
  }
  return API_BASE_URL + (image.indexOf('/') === 0 ? image : '/' + image)
}

function enrichDishCartCount(dishes, cart) {
  for (var i = 0; i < dishes.length; i += 1) {
    var count = 0
    for (var j = 0; j < cart.length; j += 1) {
      if (String(cart[j].dishId) === String(dishes[i].dishId)) {
        count = cart[j].quantity
        break
      }
    }
    dishes[i].cartCount = count
    dishes[i].cartCountText = count > 0 ? '已选 ' + count + ' 份' : ''
  }
  return dishes
}

function filterDishes(dishes, activeCat, keyword) {
  keyword = (keyword || '').toLowerCase()
  var filtered = []

  for (var i = 0; i < dishes.length; i += 1) {
    var dish = dishes[i]
    var matchCategory = !activeCat || dish.categoryIdText === String(activeCat)
    var name = dish.dishName || ''
    var desc = dish.description || ''
    var matchKeyword = !keyword ||
      name.toLowerCase().indexOf(keyword) >= 0 ||
      desc.toLowerCase().indexOf(keyword) >= 0

    if (matchCategory && matchKeyword) {
      filtered.push(dish)
    }
  }

  return filtered
}

function summarizeCart(cart) {
  var count = 0
  var total = 0

  for (var i = 0; i < cart.length; i += 1) {
    count += cart[i].quantity
    total += Number(cart[i].price || 0) * cart[i].quantity
  }

  return {
    cartCount: count,
    cartTotalText: money(total),
  }
}

function updateDishImage(dishes, dishId, updates) {
  var next = dishes.slice()
  for (var i = 0; i < next.length; i += 1) {
    if (String(next[i].dishId) === String(dishId)) {
      next[i] = Object.assign({}, next[i], updates)
      break
    }
  }
  return next
}

Page({
  data: {
    table: null,
    tableNumber: '',
    categories: [],
    dishes: [],
    filteredDishes: [],
    activeCat: '',
    keyword: '',
    cart: [],
    cartOpen: false,
    cartCount: 0,
    cartTotalText: '0.00',
    loading: false,
    submitting: false,
    callingWaiter: false,
    error: '',
  },

  onLoad: function () {
    var table = wx.getStorageSync(STORAGE_KEYS.customerTable)
    var savedCart = wx.getStorageSync(STORAGE_KEYS.customerCart)

    if (!table || !table.tableId) {
      wx.redirectTo({ url: '/pages/table/table' })
      return
    }

    this.setData({
      table: table,
      tableNumber: table.tableNumber || '未选择桌台',
      cart: Array.isArray(savedCart) ? savedCart : [],
    })
    this.updateCartSummary()
    this.loadMenu()
  },

  onPullDownRefresh: function () {
    this.loadMenu(function () {
      wx.stopPullDownRefresh()
    })
  },

  loadMenu: function (done) {
    this.setData({
      loading: true,
      error: '',
    })

    api.getMenu()
      .then(function (data) {
        var categories = Array.isArray(data && data.categories) ? data.categories : []
        var dishes = Array.isArray(data && data.dishes) ? data.dishes : []
        for (var c = 0; c < categories.length; c += 1) {
          categories[c].categoryIdText = String(categories[c].categoryId)
        }
        for (var i = 0; i < dishes.length; i += 1) {
          dishes[i].categoryIdText = String(dishes[i].categoryId)
          dishes[i].displayPrice = money(dishes[i].price)
          dishes[i].imageUrl = normalizeImage(dishes[i].image)
          dishes[i].displayImageUrl = dishes[i].imageUrl
          dishes[i].imageLoadTried = false
          dishes[i].initial = dishes[i].dishName ? dishes[i].dishName.slice(0, 1) : '菜'
          dishes[i].canAdd = Boolean(dishes[i].available && dishes[i].stock > 0)
          dishes[i].disabledClass = dishes[i].canAdd ? '' : 'disabled'
          dishes[i].showLowStock = dishes[i].stock <= 5 && dishes[i].stock > 0
          dishes[i].soldOut = dishes[i].stock <= 0
        }

        dishes = enrichDishCartCount(dishes, this.data.cart)

        this.setData({
          categories: categories,
          dishes: dishes,
          loading: false,
        })
        this.applyFilter()
        if (typeof done === 'function') done()
      }.bind(this))
      .catch(function (error) {
        this.setData({
          error: error.message || '加载菜单失败',
          loading: false,
        })
        if (typeof done === 'function') done()
      }.bind(this))
  },

  onKeywordInput: function (event) {
    this.setData({
      keyword: event.detail.value.trim(),
    })
    this.applyFilter()
  },

  onSelectCategory: function (event) {
    var categoryId = event.currentTarget.dataset.id || ''
    this.setData({
      activeCat: categoryId,
    })
    this.applyFilter()
  },

  applyFilter: function () {
    this.setData({
      filteredDishes: filterDishes(this.data.dishes, this.data.activeCat, this.data.keyword),
    })
  },

  onDishImageError: function (event) {
    var dishId = event.currentTarget.dataset.id
    var dishes = this.data.dishes
    var dish = null

    for (var i = 0; i < dishes.length; i += 1) {
      if (String(dishes[i].dishId) === String(dishId)) {
        dish = dishes[i]
        break
      }
    }

    if (!dish || !dish.imageUrl || dish.imageLoadTried) {
      return
    }

    console.warn('菜品图片加载失败，尝试下载为临时文件', dish.imageUrl, event.detail)
    this.setData({
      dishes: updateDishImage(dishes, dishId, { imageLoadTried: true }),
    })
    this.applyFilter()

    wx.downloadFile({
      url: dish.imageUrl,
      success: function (res) {
        if (res.statusCode !== 200 || !res.tempFilePath) {
          console.warn('菜品图片下载失败', dish.imageUrl, res)
          return
        }
        this.setData({
          dishes: updateDishImage(this.data.dishes, dishId, {
            displayImageUrl: res.tempFilePath,
          }),
        })
        this.applyFilter()
      }.bind(this),
      fail: function (error) {
        console.warn('菜品图片下载请求失败', dish.imageUrl, error)
      },
    })
  },

  onAddDish: function (event) {
    var index = event.currentTarget.dataset.index
    var dish = this.data.filteredDishes[index]
    var cart = this.data.cart.slice()
    var found = null

    if (!dish || !dish.available || dish.stock <= 0) {
      wx.showToast({
        title: '该菜品暂不可点',
        icon: 'none',
      })
      return
    }

    for (var i = 0; i < cart.length; i += 1) {
      if (cart[i].dishId === dish.dishId) {
        found = cart[i]
        break
      }
    }

    if (found) {
      found.quantity = Math.min(found.quantity + 1, found.stock || dish.stock || found.quantity + 1)
    } else {
      cart.push({
        dishId: dish.dishId,
        dishName: dish.dishName,
        price: Number(dish.price || 0),
        displayPrice: money(dish.price),
        stock: dish.stock,
        quantity: 1,
        remark: '',
      })
    }

    this.saveCart(cart)
  },

  onToggleCart: function () {
    this.setData({
      cartOpen: !this.data.cartOpen,
    })
  },

  onClearCart: function () {
    this.saveCart([])
    this.setData({
      cartOpen: false,
    })
  },

  onDecreaseQty: function (event) {
    this.updateCartQty(event.currentTarget.dataset.id, -1)
  },

  onIncreaseQty: function (event) {
    this.updateCartQty(event.currentTarget.dataset.id, 1)
  },

  updateCartQty: function (dishId, delta) {
    var cart = this.data.cart.slice()

    for (var i = 0; i < cart.length; i += 1) {
      if (String(cart[i].dishId) === String(dishId)) {
        cart[i].quantity += delta
        if (cart[i].quantity <= 0) {
          cart.splice(i, 1)
        } else if (cart[i].stock) {
          cart[i].quantity = Math.min(cart[i].quantity, cart[i].stock)
        }
        break
      }
    }

    this.saveCart(cart)
  },

  onRemarkInput: function (event) {
    var dishId = event.currentTarget.dataset.id
    var value = event.detail.value
    var cart = this.data.cart.slice()

    for (var i = 0; i < cart.length; i += 1) {
      if (String(cart[i].dishId) === String(dishId)) {
        cart[i].remark = value
        break
      }
    }

    this.saveCart(cart)
  },

  onCartCountInput: function (event) {
    var dishId = event.currentTarget.dataset.id
    var value = parseInt(event.detail.value, 10)
    var cart = this.data.cart.slice()

    if (isNaN(value)) {
      return
    }

    for (var i = 0; i < cart.length; i += 1) {
      if (String(cart[i].dishId) === String(dishId)) {
        if (value <= 0) {
          cart.splice(i, 1)
        } else {
          cart[i].quantity = cart[i].stock ? Math.min(value, cart[i].stock) : value
        }
        break
      }
    }

    this.saveCart(cart)
  },

  saveCart: function (cart) {
    var dishes = enrichDishCartCount(this.data.dishes.slice(), cart)
    var summary = summarizeCart(cart)
    wx.setStorageSync(STORAGE_KEYS.customerCart, cart)
    this.setData({
      cart: cart,
      dishes: dishes,
      filteredDishes: filterDishes(dishes, this.data.activeCat, this.data.keyword),
      cartCount: summary.cartCount,
      cartTotalText: summary.cartTotalText,
    })
  },

  updateCartSummary: function () {
    var summary = summarizeCart(this.data.cart)

    this.setData({
      cartCount: summary.cartCount,
      cartTotalText: summary.cartTotalText,
    })
  },

  onSubmitOrder: function () {
    var table = this.data.table
    var cart = this.data.cart
    var user = wx.getStorageSync(STORAGE_KEYS.user)
    var items = []

    if (this.data.submitting) return

    if (!table || !table.tableId) {
      wx.redirectTo({ url: '/pages/table/table' })
      return
    }

    if (!cart.length) {
      wx.showToast({
        title: '请先选择菜品',
        icon: 'none',
      })
      return
    }

    for (var i = 0; i < cart.length; i += 1) {
      items.push({
        dishId: cart[i].dishId,
        quantity: cart[i].quantity,
        remark: cart[i].remark || '',
      })
    }

    this.setData({
      submitting: true,
      error: '',
    })

    api.placeOrder({
      tableId: table.tableId,
      userId: user && user.userId,
      items: items,
    })
      .then(function (order) {
        wx.setStorageSync(STORAGE_KEYS.currentOrder, order)
        wx.removeStorageSync(STORAGE_KEYS.customerCart)
        this.setData({
          cart: [],
          cartOpen: false,
          submitting: false,
        })
        this.updateCartSummary()
        wx.navigateTo({ url: '/pages/order/order' })
      }.bind(this))
      .catch(function (error) {
        this.setData({
          error: error.message || '下单失败',
          submitting: false,
        })
      }.bind(this))
  },

  onGoOrder: function () {
    wx.navigateTo({ url: '/pages/order/order' })
  },

  onSwitchTable: function () {
    wx.showModal({
      title: '切换桌台',
      content: '切换桌台会清空当前未下单购物车，确认继续吗？',
      confirmText: '切换',
      success: function (res) {
        if (!res.confirm) return
        wx.removeStorageSync(STORAGE_KEYS.customerTable)
        wx.removeStorageSync(STORAGE_KEYS.customerCart)
        wx.removeStorageSync(STORAGE_KEYS.currentOrder)
        wx.redirectTo({ url: '/pages/table/table' })
      },
    })
  },

  onCallWaiter: function () {
    var table = this.data.table
    var user = wx.getStorageSync(STORAGE_KEYS.user)

    if (!table || !table.tableId || this.data.callingWaiter) return

    this.setData({
      callingWaiter: true,
      error: '',
    })

    api.callWaiter(table.tableId, user && user.userId)
      .then(function () {
        wx.showToast({
          title: '已呼叫服务员',
          icon: 'success',
        })
        this.setData({
          callingWaiter: false,
        })
      }.bind(this))
      .catch(function (error) {
        this.setData({
          callingWaiter: false,
          error: error.message || '呼叫失败',
        })
      }.bind(this))
  },
})
