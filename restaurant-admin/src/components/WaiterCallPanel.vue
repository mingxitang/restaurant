<template>
  <section class="waiter-call-panel">
    <div class="waiter-call-head">
      <div>
        <h3>服务呼叫</h3>
        <p>顾客通过小程序呼叫服务员后会显示在这里</p>
      </div>
      <button class="ghost" @click="$emit('refresh')">刷新</button>
    </div>
    <div v-if="calls.length" class="waiter-call-list">
      <article v-for="call in calls" :key="call.callId" class="waiter-call-item">
        <div>
          <strong>{{ call.tableNumber || ('桌台 #' + call.tableId) }}</strong>
          <span>{{ call.remark || '顾客呼叫服务员' }}</span>
          <small>{{ call.callTime }}</small>
        </div>
        <button @click="$emit('complete', call)">已处理</button>
      </article>
    </div>
    <p v-else class="compact-empty">暂无待处理服务呼叫</p>
  </section>
</template>

<script setup>
defineProps({
  calls: {
    type: Array,
    default: () => []
  }
})

defineEmits(['refresh', 'complete'])
</script>

<style scoped>
.waiter-call-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  margin: 18px 0;
}

.waiter-call-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.waiter-call-head h3 {
  margin: 0;
}

.waiter-call-head p {
  color: #6b7280;
  margin: 4px 0 0;
}

.waiter-call-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}

.waiter-call-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 8px;
  padding: 12px;
}

.waiter-call-item strong,
.waiter-call-item span,
.waiter-call-item small {
  display: block;
}

.waiter-call-item span {
  color: #92400e;
  margin-top: 4px;
}

.waiter-call-item small {
  color: #6b7280;
  margin-top: 4px;
}

.compact-empty {
  color: #6b7280;
  margin: 0;
}
</style>
