<template>
  <div>
    <header class="app-header">
      <h2>{{ isReview ? '评价' : '支付' }}</h2>
      <button @click="router.push('/order')">返回</button>
    </header>
    <div class="pay-page">
      <div v-if="!isReview">
        <div class="cart-summary">
          <div class="row"><span>订单金额</span><span>&yen;{{ order?.totalAmount?.toFixed(2) || '0.00' }}</span></div>
          <div class="row total"><span>应付</span><span>&yen;{{ order?.totalAmount?.toFixed(2) || '0.00' }}</span></div>
        </div>

        <h3 style="margin-top:20px">选择支付方式</h3>
        <div class="pay-methods">
          <button
            v-for="m in methods"
            :key="m.value"
            :class="{ active: method === m.value }"
            @click="method = m.value"
          >{{ m.label }}</button>
        </div>

        <button class="primary" style="width:100%;padding:14px;font-size:16px;margin-top:16px" :disabled="paying" @click="doPay">
          {{ paying ? '支付中...' : '确认支付 ¥' + (order?.totalAmount?.toFixed(2) || '0.00') }}
        </button>
        <p v-if="error" class="error" style="text-align:center;margin-top:12px">{{ error }}</p>
      </div>

      <div v-else class="review-section">
        <h4>请为本次用餐评分</h4>
        <div class="stars">
          <button v-for="n in 5" :key="n" :class="{ on: rating >= n }" @click="rating = n">{{ rating >= n ? '★' : '☆' }}</button>
        </div>
        <textarea v-model="comment" placeholder="说说您的用餐体验..."></textarea>
        <button class="primary" style="width:100%;padding:14px;font-size:16px;margin-top:16px" :disabled="submitting" @click="doReview">
          {{ submitting ? '提交中...' : '提交评价' }}
        </button>
        <button class="ghost" style="width:100%;margin-top:8px" @click="skipReview">跳过</button>
        <p v-if="error" class="error" style="text-align:center;margin-top:12px">{{ error }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getOrder, payOrder, reviewOrder } from '../api'
import { store } from '../store'

const router = useRouter()
const route = useRoute()
const order = ref(null)
const method = ref('微信支付')
const paying = ref(false)
const submitting = ref(false)
const error = ref('')
const rating = ref(5)
const comment = ref('')

const methods = [
  { label: '微信支付', value: '微信支付' },
  { label: '支付宝', value: '支付宝' },
  { label: '现金', value: '现金' }
]

const isReview = computed(() => route.query.review === '1')

async function doPay() {
  paying.value = true
  error.value = ''
  try {
    const orderId = order.value?.orderId || store.currentOrder?.orderId
    await payOrder(orderId, { payMethod: method.value, discountAmount: 0, pointsToUse: 0 })
    router.push({ path: '/pay', query: { review: '1' } })
  } catch (err) {
    error.value = err.message || '支付失败'
  } finally {
    paying.value = false
  }
}

async function doReview() {
  submitting.value = true
  error.value = ''
  try {
    const orderId = order.value?.orderId || store.currentOrder?.orderId
    await reviewOrder(orderId, { orderId, rating: rating.value, comment: comment.value })
    store.currentOrder = null
    store.tableId = null
    store.tableNumber = ''
    router.push('/')
  } catch (err) {
    error.value = err.message || '提交评价失败'
  } finally {
    submitting.value = false
  }
}

function skipReview() {
  store.currentOrder = null
  store.tableId = null
  store.tableNumber = ''
  router.push('/')
}

onMounted(async () => {
  const orderId = store.currentOrder?.orderId
  if (!orderId) {
    router.push('/order')
    return
  }
  try {
    order.value = await getOrder(orderId)
  } catch (e) {
    order.value = store.currentOrder
  }
})
</script>
