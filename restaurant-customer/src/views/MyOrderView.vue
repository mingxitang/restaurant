<template>
  <div>
    <header class="app-header">
      <h2>我的订单</h2>
      <button @click="loadOrder">刷新</button>
    </header>
    <div class="order-page">
      <div v-if="loading">
        <p style="text-align:center;color:#999;padding:40px">加载中...</p>
      </div>
      <div v-else-if="order">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
          <span>桌号：<strong>{{ store.tableNumber }}</strong></span>
          <span :class="'order-status ' + String(order.status || '').toLowerCase()">{{ statusText(order.status) }}</span>
        </div>

        <div class="order-item" v-for="detail in order.details" :key="detail.dishId">
          <div>
            <strong>{{ detail.dishName }}</strong>
            <div style="font-size:12px;color:#999">&yen;{{ detail.unitPrice?.toFixed(2) }} x {{ detail.quantity }}</div>
            <small v-if="detail.remark" style="font-size:12px;color:#999">{{ detail.remark }}</small>
            <small v-if="detail.status" :class="'cooking-status ' + detail.status.toLowerCase()">{{ cookingText(detail.status) }}</small>
          </div>
          <strong>&yen;{{ ((detail.unitPrice || 0) * detail.quantity).toFixed(2) }}</strong>
        </div>

        <div class="cart-summary" style="margin-top:16px">
          <div class="row total"><span>合计</span><span>&yen;{{ order.totalAmount?.toFixed(2) || '0.00' }}</span></div>
        </div>

        <div class="reminder-box" v-if="order.status === 'PENDING' || order.status === 'PAID'">
          <div>
            <strong>催单提醒</strong>
            <span v-if="order.reminderCount">已催单 {{ order.reminderCount }} 次</span>
            <span v-else>菜品久等时可以提醒厨房</span>
          </div>
          <button class="ghost" :disabled="reminding" @click="remind">
            {{ reminding ? '提交中...' : '催单' }}
          </button>
        </div>
        <p v-if="message" class="order-tip">{{ message }}</p>

        <div class="cart-actions order-actions" v-if="order.status === 'PENDING'">
          <button class="order-add-btn" @click="router.push('/menu')">再加菜</button>
          <button class="order-pay-btn" @click="goPay">去支付</button>
        </div>

        <div style="margin-top:12px" v-if="order.status === 'PAID' || order.status === 'COMPLETED'">
          <button class="primary" style="width:100%" @click="goReview">{{ order.reviewed ? '查看评价' : '去评价' }}</button>
        </div>
      </div>
      <div v-else class="empty-cart">
        <p>暂无订单，请先点菜下单。</p>
        <button class="primary" style="margin-top:12px" @click="router.push('/menu')">去点菜</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrder, remindOrder } from '../api'
import { store } from '../store'

const router = useRouter()
const order = ref(null)
const loading = ref(false)
const reminding = ref(false)
const message = ref('')
let refreshTimer = null

function statusText(s) {
  const map = { PENDING: '待支付', PAID: '已支付', COMPLETED: '已完成', CANCELLED: '已取消' }
  return map[s] || s
}

function cookingText(s) {
  const map = { PENDING: '待制作', PREPARING: '制作中', READY: '待上菜', SERVED: '已上菜' }
  return map[s] || s
}

function goPay() {
  router.push('/pay')
}

function goReview() {
  router.push('/pay?review=1')
}

async function remind() {
  const orderId = order.value?.orderId || store.currentOrder?.orderId
  if (!orderId) return
  reminding.value = true
  message.value = ''
  try {
    await remindOrder(orderId)
    message.value = '已帮您催单，厨房看板会收到提醒。'
    await loadOrder(true)
  } catch (err) {
    message.value = err.message || '催单失败'
  } finally {
    reminding.value = false
  }
}

async function loadOrder(silent = false) {
  if (!store.currentOrder?.orderId && !store.tableId) {
    router.push('/')
    return
  }
  if (!silent) loading.value = true
  try {
    const orderId = store.currentOrder?.orderId
    if (orderId) {
      order.value = await getOrder(orderId)
      store.currentOrder = order.value
    }
  } catch (e) {
    // order might not exist yet
    order.value = store.currentOrder
  } finally {
    if (!silent) loading.value = false
  }
}

onMounted(() => {
  loadOrder()
  refreshTimer = setInterval(() => loadOrder(true), 15000)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})
</script>
