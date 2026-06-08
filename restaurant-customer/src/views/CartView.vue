<template>
  <div>
    <header class="app-header">
      <h2>购物车</h2>
      <button @click="router.push('/menu')">返回菜单</button>
    </header>

    <div class="cart-page">
      <div v-if="store.cart.length">
        <div class="cart-page-head">
          <h3>未下单菜品</h3>
          <button class="ghost" @click="store.clearCart()">清空</button>
        </div>

        <div class="cart-item" v-for="item in store.cart" :key="item.dishId">
          <div class="info">
            <h4>{{ item.dishName }}</h4>
            <p>&yen;{{ Number(item.price || 0).toFixed(2) }} / 份</p>
            <input
              :value="item.remark"
              placeholder="备注：少辣、不要葱、打包等"
              @input="store.updateCartRemark(item.dishId, $event.target.value)"
            />
          </div>
          <div class="qty">
            <button @click="store.updateCartQty(item.dishId, item.quantity - 1)">-</button>
            <span>{{ item.quantity }}</span>
            <button @click="store.updateCartQty(item.dishId, item.quantity + 1)">+</button>
          </div>
        </div>

        <div class="cart-summary">
          <div class="row total"><span>合计</span><span>&yen;{{ store.cartTotal.toFixed(2) }}</span></div>
        </div>

        <div class="cart-actions">
          <button class="ghost" @click="router.push('/menu')">继续加菜</button>
          <button class="primary" :disabled="submitting" @click="submitOrder">
            {{ submitting ? '下单中...' : '提交订单' }}
          </button>
        </div>
        <p v-if="error" class="error floating-error">{{ error }}</p>
      </div>

      <div v-else class="empty-cart">
        <p>购物车是空的</p>
        <div class="empty-cart-actions">
          <button class="primary" @click="router.push('/menu')">去点菜</button>
          <button class="ghost" v-if="store.currentOrder?.orderId" @click="router.push('/order')">查看订单</button>
        </div>
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
  if (!store.tableId) {
    router.push('/')
    return
  }
  submitting.value = true
  error.value = ''
  try {
    const data = await placeOrder({
      tableId: store.tableId,
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
