<template>
  <div>
    <header class="app-header">
      <h2>我的订单</h2>
      <button @click="router.push('/menu')">继续点菜</button>
    </header>
    <div class="order-page">
      <div v-if="!order">
        <p style="text-align:center;color:#999;padding:40px">加载中...</p>
      </div>
      <div v-else>
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
          <span>桌号：<strong>{{ store.tableNumber }}</strong></span>
          <span :class="'order-status ' + order.status.toLowerCase()">{{ statusText(order.status) }}</span>
        </div>

        <div class="order-item" v-for="detail in order.details" :key="detail.dishId">
          <div>
            <strong>{{ detail.dishName }}</strong>
            <div style="font-size:12px;color:#999">&yen;{{ detail.unitPrice?.toFixed(2) }} x {{ detail.quantity }}</div>
          </div>
          <strong>&yen;{{ ((detail.unitPrice || 0) * detail.quantity).toFixed(2) }}</strong>
        </div>

        <div class="cart-summary" style="margin-top:16px">
          <div class="row total"><span>合计</span><span>&yen;{{ order.totalAmount?.toFixed(2) || '0.00' }}</span></div>
        </div>

        <div style="margin-top:16px;display:flex;gap:12px">
          <button class="primary" style="flex:1" @click="goPay" v-if="order.status === 'PENDING'">去支付</button>
          <button style="flex:1;background:#333;color:#fff" @click="router.push('/menu')" v-if="order.status === 'PENDING'">再加菜</button>
        </div>

        <div style="margin-top:12px" v-if="order.status === 'PAID' || order.status === 'COMPLETED'">
          <button class="primary" style="width:100%" @click="goReview">{{ order.reviewed ? '查看评价' : '去评价' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrder } from '../api'
import { store } from '../store'

const router = useRouter()
const order = ref(null)

function statusText(s) {
  const map = { PENDING: '待支付', PAID: '已支付', COMPLETED: '已完成', CANCELLED: '已取消' }
  return map[s] || s
}

function goPay() {
  router.push('/pay')
}

function goReview() {
  router.push('/pay?review=1')
}

onMounted(async () => {
  if (!store.currentOrder?.orderId && !store.tableId) {
    router.push('/')
    return
  }
  try {
    const orderId = store.currentOrder?.orderId
    if (orderId) {
      order.value = await getOrder(orderId)
    }
  } catch (e) {
    // order might not exist yet
    order.value = store.currentOrder
  }
})
</script>
