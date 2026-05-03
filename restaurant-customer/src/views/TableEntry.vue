<template>
  <div>
    <header class="app-header">
      <h2>选择桌台</h2>
      <button @click="logout">退出</button>
    </header>
    <div class="table-entry">
      <h3>请选择您所在的桌台</h3>
      <div class="table-grid" v-if="tables.length">
        <div
          v-for="table in tables"
          :key="table.tableId"
          class="table-card"
          :class="{ occupied: table.status !== 'FREE' }"
          @click="table.status === 'FREE' && selectTable(table)"
        >
          <strong>{{ table.tableName || table.tableNumber }}</strong>
          <span>{{ table.area || '大厅' }} · {{ table.capacity }}人</span>
          <span>{{ table.status === 'FREE' ? '空闲' : '占用' }}</span>
        </div>
      </div>
      <div class="manual-input">
        <input v-model.trim="manualNumber" placeholder="或手动输入桌号" />
        <button @click="manualSelect">确认</button>
      </div>
      <p v-if="error" class="error" style="margin-top:12px">{{ error }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listTables } from '../api'
import { store } from '../store'

const router = useRouter()
const tables = ref([])
const manualNumber = ref('')
const error = ref('')

onMounted(async () => {
  try {
    tables.value = await listTables()
  } catch (e) {
    error.value = '加载桌台失败'
  }
})

function selectTable(table) {
  store.tableId = table.tableId
  store.tableNumber = table.tableName || table.tableNumber
  router.push('/menu')
}

function manualSelect() {
  if (!manualNumber.value) return
  const found = tables.value.find(
    t => t.tableNumber === manualNumber.value || t.tableName === manualNumber.value
  )
  if (found) {
    selectTable(found)
  } else {
    error.value = '未找到该桌台，请检查桌号'
  }
}

function logout() {
  localStorage.clear()
  store.clearCart()
  router.push('/login')
}
</script>
