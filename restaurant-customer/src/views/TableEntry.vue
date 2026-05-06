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
import { useRoute, useRouter } from 'vue-router'
import { listTables, logout as logoutApi } from '../api'
import { store } from '../store'

const router = useRouter()
const route = useRoute()
const tables = ref([])
const manualNumber = ref('')
const error = ref('')

onMounted(async () => {
  try {
    tables.value = await listTables()
    autoSelectFromQuery()
  } catch (e) {
    error.value = '加载桌台失败'
  }
})

function selectTable(table) {
  store.setTable(table)
  router.push('/menu')
}

function autoSelectFromQuery() {
  const tableValue = route.query.table || route.query.tableNumber || route.query.tableId
  if (!tableValue) return
  manualNumber.value = String(tableValue)
  const found = findTable(manualNumber.value)
  if (found) {
    selectTable(found)
  } else {
    error.value = '扫码桌号未找到，请手动选择桌台'
  }
}

function findTable(value) {
  return tables.value.find(t =>
    String(t.tableId) === value ||
    t.tableNumber === value ||
    t.tableName === value
  )
}

function manualSelect() {
  if (!manualNumber.value) return
  const found = findTable(manualNumber.value)
  if (found) {
    selectTable(found)
  } else {
    error.value = '未找到该桌台，请检查桌号'
  }
}

async function logout() {
  try { await logoutApi() } catch (_) { /* 请求失败也不阻塞退出 */ }
  localStorage.clear()
  store.clearCart()
  store.clearTable()
  router.push('/login')
}
</script>
