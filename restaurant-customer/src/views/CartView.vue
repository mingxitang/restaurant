<template>
  <div>
    <header class="app-header">
      <h2>购物车</h2>
      <button @click="router.push('/menu')">继续点菜</button>
    </header>
    <div class="cart-page">
      <div v-if="store.cart.length === 0" style="text-align:center;padding:60px;color:#999">
        <p>购物车为空</p>
        <button class="primary" style="margin-top:16px" @click="router.push('/menu')">去点菜</button>
      </div>

      <div v-else>
        <div class="cart-item" v-for="item in store.cart" :key="item.dishId">
          <div class="info">
            <h4>{{ item.dishName }}</h4>
            <p>&yen;{{ item.price.toFixed(2) }} / 份</p>
          </div>
          <div class="qty">
            <button @click="store.updateCartQty(item.dishId, item.quantity - 1)">-</button>
            <span>{{ item.quantity }}</span>
            <button @click="store.updateCartQty(item.dishId, item.quantity + 1)">+</button>
          </div>
        </div>

        <div class="cart-summary">
          <div class="row"><span>共 {{ store.cartCount }} 份</span></div>
          <div class="row total"><span>合计</span><span>&yen;{{ store.cartTotal.toFixed(2) }}</span></div>
        </div>

        <div class="cart-actions">
          <button class="primary" @click="submitOrder" :disabled="submitting">
            {{ submitting ? '提交中...' : '确认下单' }}
          </button>
        </div>
        <p v-if="error" class="error" style="text-align:center;margin-top:12px">{{ error }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { placeOrder } from '../api'
import { store } from '../store'

const router = useRouter()
const submitting = ref(false)
const error = ref('')

async function submitOrder() {
  if (!store.tableId) return router.push('/')
  submitting.value = true
  error.value = ''
  try {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    const data = await placeOrder({
      tableId: store.tableId,
      userId: user?.userId,
      items: store.cart.map(item => ({
        dishId: item.dishId,
        quantity: item.quantity,
        remark: item.remark
      }))
    })
    store.currentOrder = data
    store.clearCart()
    router.push('/order')
  } catch (err) {
    error.value = err.message || '下单失败'
  } finally {
    submitting.value = false
  }
}
</script>
