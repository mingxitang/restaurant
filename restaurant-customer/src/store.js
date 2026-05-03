import { reactive } from 'vue'

export const store = reactive({
  tableId: null,
  tableNumber: '',
  currentOrder: null,
  cart: [],
  addToCart(dish) {
    const existing = this.cart.find(item => item.dishId === dish.dishId)
    if (existing) {
      existing.quantity++
    } else {
      this.cart.push({
        dishId: dish.dishId,
        dishName: dish.dishName,
        price: dish.price,
        quantity: 1,
        remark: ''
      })
    }
  },
  removeFromCart(dishId) {
    this.cart = this.cart.filter(item => item.dishId !== dishId)
  },
  updateCartQty(dishId, qty) {
    const item = this.cart.find(i => i.dishId === dishId)
    if (item) {
      if (qty <= 0) {
        this.removeFromCart(dishId)
      } else {
        item.quantity = qty
      }
    }
  },
  clearCart() {
    this.cart = []
  },
  get cartCount() {
    return this.cart.reduce((sum, item) => sum + item.quantity, 0)
  },
  get cartTotal() {
    return this.cart.reduce((sum, item) => sum + item.price * item.quantity, 0)
  }
})
