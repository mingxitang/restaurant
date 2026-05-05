import { reactive } from 'vue'

const savedTable = JSON.parse(localStorage.getItem('customerTable') || 'null')
const savedCart = JSON.parse(localStorage.getItem('customerCart') || '[]')

export const store = reactive({
  tableId: savedTable?.tableId || null,
  tableNumber: savedTable?.tableNumber || '',
  currentOrder: null,
  cart: Array.isArray(savedCart) ? savedCart : [],
  setTable(table) {
    this.tableId = table.tableId
    this.tableNumber = table.tableName || table.tableNumber
    localStorage.setItem('customerTable', JSON.stringify({
      tableId: this.tableId,
      tableNumber: this.tableNumber
    }))
  },
  clearTable() {
    this.tableId = null
    this.tableNumber = ''
    localStorage.removeItem('customerTable')
  },
  saveCart() {
    localStorage.setItem('customerCart', JSON.stringify(this.cart))
  },
  addToCart(dish) {
    const existing = this.cart.find(item => item.dishId === dish.dishId)
    if (existing) {
      existing.quantity = Math.min(existing.quantity + 1, existing.stock || dish.stock || existing.quantity + 1)
    } else {
      this.cart.push({
        dishId: dish.dishId,
        dishName: dish.dishName,
        price: dish.price,
        stock: dish.stock,
        quantity: 1,
        remark: ''
      })
    }
    this.saveCart()
  },
  removeFromCart(dishId) {
    this.cart = this.cart.filter(item => item.dishId !== dishId)
    this.saveCart()
  },
  updateCartQty(dishId, qty) {
    const item = this.cart.find(i => i.dishId === dishId)
    if (item) {
      if (qty <= 0) {
        this.removeFromCart(dishId)
      } else {
        item.quantity = Math.min(qty, item.stock || qty)
        this.saveCart()
      }
    }
  },
  updateCartRemark(dishId, remark) {
    const item = this.cart.find(i => i.dishId === dishId)
    if (item) {
      item.remark = remark
      this.saveCart()
    }
  },
  clearCart() {
    this.cart = []
    localStorage.removeItem('customerCart')
  },
  get cartCount() {
    return this.cart.reduce((sum, item) => sum + item.quantity, 0)
  },
  get cartTotal() {
    return this.cart.reduce((sum, item) => sum + item.price * item.quantity, 0)
  }
})
