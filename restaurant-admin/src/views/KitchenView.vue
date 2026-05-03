<template>
  <section class="kitchen">
    <h2>厨房看板 <small style="font-weight:400;font-size:14px;color:#999">共 {{ queue.length }} 项待处理</small></h2>
    <div class="kitchen-stats">
      <article><span>待制作</span><strong>{{ groupedQueue.reduce((s, g) => s + (g.cookingStatus === 'PENDING' || g.cookingStatus === 'PREPARING' ? g.quantity : 0), 0) }}</strong></article>
      <article><span>已完成</span><strong>{{ groupedQueue.reduce((s, g) => s + (g.cookingStatus === 'READY' ? g.quantity : 0), 0) }}</strong></article>
    </div>

    <div class="kitchen-grid" v-if="groupedQueue.length">
      <article
        v-for="group in groupedQueue"
        :key="group.dishId"
        class="kitchen-card"
        :class="urgencyClass(group.waitMinutes)"
      >
        <header>
          <strong>{{ group.dishName }}</strong>
          <em class="urgency">{{ group.waitMinutes }}min</em>
        </header>
        <div class="order-refs">
          <span v-for="ref in group.orders" :key="ref.orderId" class="table-tag">
            {{ ref.tableNumber }} x{{ ref.quantity }}
          </span>
        </div>
        <div class="status-row">
          <span :class="'status-badge ' + (group.cookingStatus || 'PENDING').toLowerCase()">
            {{ statusText(group.cookingStatus) }}
          </span>
        </div>
        <div class="actions">
          <button
            v-if="!group.cookingStatus || group.cookingStatus === 'PENDING' || group.cookingStatus === 'PREPARING'"
            class="primary"
            @click="batchUpdate(group, group.cookingStatus === 'PENDING' ? 'PREPARING' : 'READY')"
          >
            {{ group.cookingStatus === 'PENDING' ? '开始制作' : '制作完成' }}
          </button>
          <button
            v-if="group.cookingStatus === 'READY'"
            class="ghost"
            @click="batchUpdate(group, 'SERVED')"
          >
            已上菜
          </button>
        </div>
      </article>
    </div>
    <p v-else style="text-align:center;color:#999;padding:60px">暂无待处理订单</p>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import http from '../api/http'

const queue = ref([])
let timer = null

const groupedQueue = computed(() => {
  const grouped = {}
  for (const item of queue.value) {
    const key = item.dishId
    if (!grouped[key]) {
      grouped[key] = {
        dishId: item.dishId,
        dishName: item.dishName,
        cookingStatus: item.cookingStatus || 'PENDING',
        waitMinutes: item.waitMinutes || 0,
        quantity: 0,
        orders: []
      }
    }
    grouped[key].orders.push({
      orderId: item.orderId,
      tableNumber: item.tableNumber,
      quantity: item.quantity
    })
    grouped[key].quantity += item.quantity
    grouped[key].waitMinutes = Math.max(grouped[key].waitMinutes, item.waitMinutes || 0)
  }
  const result = Object.values(grouped)
  result.sort((a, b) => {
    const statusOrder = { PENDING: 0, PREPARING: 1, READY: 2, SERVED: 3 }
    const sa = statusOrder[a.cookingStatus] ?? 0
    const sb = statusOrder[b.cookingStatus] ?? 0
    if (sa !== sb) return sa - sb
    return b.waitMinutes - a.waitMinutes
  })
  return result
})

function urgencyClass(min) {
  if (min >= 20) return 'urgent'
  if (min >= 10) return 'warn'
  return 'ok'
}

function statusText(s) {
  const map = { PENDING: '待制作', PREPARING: '制作中', READY: '已完成', SERVED: '已上菜' }
  return map[s] || '待制作'
}

async function load() {
  try {
    const res = await http.get('/kitchen/queue')
    queue.value = res || []
  } catch (e) {
    // silently fail
  }
}

async function batchUpdate(group, status) {
  for (const order of group.orders) {
    try {
      await http.put(`/kitchen/${order.orderId}/${group.dishId}/status`, { status })
    } catch (e) {
      // continue
    }
  }
  await load()
}

onMounted(() => {
  load()
  timer = setInterval(load, 15000)
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>
