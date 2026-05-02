<template>
  <section class="frontdesk">
    <h2>运营首页</h2>
    <p v-if="message && !activeTable" class="order-message">{{ message }}</p>
    <div class="stats">
      <article class="clickable-stat" @click="showTodayOrders = true">
        <span>今日订单</span>
        <strong>{{ data.todayOrders || 0 }}</strong>
        <small v-if="todayUnpaidCount > 0">未结 {{ todayUnpaidCount }}</small>
      </article>
      <article class="clickable-stat revenue-card" @click="showMonthlyStats = true">
        <span>今日营业额</span>
        <div><b>已收</b><strong>¥{{ todayPaidAmount.toFixed(2) }}</strong></div>
        <div><b>未收</b><strong>¥{{ todayUnpaidAmount.toFixed(2) }}</strong></div>
        <div><b>合计</b><strong>¥{{ todayTotalAmount.toFixed(2) }}</strong></div>
      </article>
      <article><span>空闲桌位</span><strong>{{ data.freeTables || 0 }}</strong></article>
      <article><span>低库存菜品</span><strong>{{ data.lowStockDishes || 0 }}</strong></article>
    </div>

    <div class="floor-head">
      <h3>桌台</h3>
      <div class="legend">
        <span><i class="dot free"></i>空闲</span>
        <span><i class="dot occupied"></i>占用</span>
        <span><i class="dot reserved"></i>预订</span>
        <span><i class="dot cleaning"></i>待清扫</span>
        <button class="ghost" @click="showAddTable = !showAddTable">{{ showAddTable ? '取消添加' : '+ 添加桌台' }}</button>
      </div>
    </div>

    <div v-if="showAddTable" class="editor" style="margin-bottom:14px">
      <input v-model.trim="newTableForm.tableNumber" placeholder="桌号（编号）" />
      <input v-model.trim="newTableForm.tableName" placeholder="桌台名称（可选）" />
      <input v-model.trim="newTableForm.area" placeholder="区域" />
      <input v-model.number="newTableForm.capacity" type="number" min="1" max="20" placeholder="人数" />
      <button @click="addTable">确认添加</button>
    </div>

    <div class="floor-grid">
      <div
        v-for="table in tables"
        :key="table.tableId"
        class="floor-table"
        :class="table.status.toLowerCase()"
        @click.stop="selectTable(table)"
      >
        <template v-if="editingTableId === table.tableId">
          <input v-model.trim="editTableForm.tableName" :placeholder="table.tableNumber" @click.stop />
          <input v-model.trim="editTableForm.area" placeholder="区域" @click.stop />
          <input v-model.number="editTableForm.capacity" type="number" min="1" max="20" @click.stop />
          <div class="edit-actions">
            <button class="ghost" @click.stop="saveEditTable(table)">保存</button>
            <button class="ghost" @click.stop="cancelEditTable">取消</button>
          </div>
        </template>
        <template v-else-if="pendingOpenTableId === table.tableId">
          <button class="open-inline open-only" @click.stop="openTableFromCard(table)">开台</button>
        </template>
        <template v-else>
          <strong>{{ table.tableName || table.tableNumber }}</strong>
          <span>{{ table.area || '大厅' }} · {{ table.capacity }}人</span>
          <em>{{ statusText(table.status) }}</em>
          <button class="ghost table-edit-btn" @click.stop="startEditTable(table)">改名</button>
        </template>
      </div>
    </div>

    <div v-if="activeTable" class="modal-mask" @click.self="closeTableModal">
      <div class="desk-modal">
        <header>
          <div>
            <h3>{{ activeTable.tableNumber }}</h3>
            <p>{{ activeTable.area || '大厅' }} · {{ activeTable.capacity }}人 · {{ statusText(activeTable.status) }}</p>
          </div>
          <button class="ghost" @click="closeTableModal">关闭</button>
        </header>

        <div v-if="activeTable.status !== 'FREE'" class="modal-body">
          <div class="order-menu">
            <div class="dish-search">
              <input v-model.trim="dishKeyword" placeholder="搜索菜品名称、分类或描述" />
              <button class="ghost" @click="dishKeyword = ''">清空</button>
            </div>
            <div class="category-tabs compact">
              <button :class="{ active: activeCategoryId === null }" class="ghost" @click="activeCategoryId = null">全部</button>
              <button
                v-for="category in categories"
                :key="category.categoryId"
                :class="{ active: activeCategoryId === category.categoryId }"
                class="ghost"
                @click="activeCategoryId = category.categoryId"
              >
                {{ category.categoryName }}
              </button>
            </div>
            <div class="compact-dish-grid">
              <article
                v-for="dish in filteredDishes"
                :key="dish.dishId"
                :class="{ disabled: !dish.available || dish.stock <= 0 }"
                @click="dish.available && dish.stock > 0 && addToCart(dish)"
              >
                <div>
                  <strong>{{ dish.dishName }}</strong>
                  <span>¥{{ dish.price }} · 库存 {{ dish.stock }}</span>
                </div>
                <em>{{ dish.stock <= 0 ? '售罄' : '单击点菜' }}</em>
              </article>
            </div>
          </div>

          <aside class="desk-cart">
            <div class="checkout-strip">
              <div>
                <span>桌台应收</span>
                <strong>¥{{ activeTotal.toFixed(2) }}</strong>
              </div>
              <div class="checkout-actions">
                <button class="ghost" @click="closeTableWithoutCheckout">关台</button>
                <button :disabled="activeOrders.length === 0" @click="checkout">结账</button>
              </div>
            </div>

            <div class="cart-section">
              <div class="cart-section-head">
                <h4>本次加菜</h4>
                <button :disabled="cart.length === 0" @click="submitOrder">{{ activeOrders.length ? '提交加菜' : '提交点餐' }}</button>
              </div>
              <p v-if="cart.length === 0" class="compact-empty">单击左侧菜品即可点菜</p>
              <div v-for="item in cart" :key="item.dishId" class="desk-cart-item cart-edit">
                <div class="line-main">
                  <strong>{{ item.dishName }}</strong>
                  <span>¥{{ item.price }}</span>
                </div>
                <div class="line-controls">
                  <input v-model.number="item.quantity" type="number" min="1" :max="item.stock" />
                  <input v-model.trim="item.remark" placeholder="备注" />
                  <button class="danger" @click="removeCartItem(item.dishId)">退</button>
                </div>
              </div>
            </div>

            <div class="cart-section ordered-section">
              <div class="cart-section-head">
                <h4>已点菜品</h4>
                <div class="section-actions">
                  <button class="ghost" @click="loadActiveOrders">刷新</button>
                  <button class="danger" :disabled="selectedRefundDetails.length === 0" @click="refundSelected">批量退菜</button>
                </div>
              </div>
              <div class="dish-search compact-search">
                <input v-model.trim="refundKeyword" placeholder="搜索已点菜品" />
                <button class="ghost" @click="refundKeyword = ''">清空</button>
              </div>
              <p v-if="filteredOrderedDetails.length === 0" class="compact-empty">该桌暂无已点菜品</p>
              <div v-for="detail in filteredOrderedDetails" :key="detail.dishId" class="ordered-item">
                <div class="line-main">
                  <label class="select-line">
                    <input :checked="detail.selected" type="checkbox" @change="updateRefundDraft(detail, 'selected', $event.target.checked)" />
                    <strong>{{ detail.dishName }}</strong>
                  </label>
                  <span>{{ detail.quantity }}份 · ¥{{ detail.subtotal.toFixed(2) }}</span>
                  <small v-if="detail.remark">{{ detail.remark }}</small>
                </div>
                <div class="line-controls">
                  <input :value="detail.refundQuantity" type="number" min="1" :max="detail.quantity" placeholder="退菜份数" @input="updateRefundDraft(detail, 'refundQuantity', Number($event.target.value))" />
                  <input :value="detail.refundReason" placeholder="退菜原因" @input="updateRefundDraft(detail, 'refundReason', $event.target.value)" />
                  <button class="danger" @click="refundDetail(detail)">退菜</button>
                </div>
              </div>
            </div>
          </aside>
        </div>

        <p v-if="message" class="order-message">{{ message }}</p>
      </div>
    </div>

    <div v-if="showTodayOrders" class="modal-mask" @click.self="showTodayOrders = false">
      <div class="orders-modal">
        <header>
          <h3>今日订单</h3>
          <button class="ghost" @click="showTodayOrders = false">关闭</button>
        </header>
        <div class="order-filters">
          <input v-model.trim="orderFilters.keyword" placeholder="搜索订单号、桌号、用户" />
          <input v-model.number="orderFilters.minAmount" type="number" min="0" placeholder="最低金额" />
          <input v-model.number="orderFilters.maxAmount" type="number" min="0" placeholder="最高金额" />
          <input v-model="orderFilters.startTime" type="datetime-local" />
          <input v-model="orderFilters.endTime" type="datetime-local" />
          <select v-model="orderFilters.orderState">
            <option value="">全部订单</option>
            <option value="PENDING">待支付</option>
            <option value="PAID">已支付</option>
            <option value="COMPLETED">已完成</option>
            <option value="CANCELLED">已取消</option>
          </select>
          <button class="ghost" @click="resetOrderFilters">重置</button>
        </div>
        <table>
          <thead><tr><th>ID</th><th>桌号</th><th>用户</th><th>金额</th><th>状态</th><th>支付</th><th>时间</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="order in filteredTodayOrders" :key="order.orderId">
              <td>{{ order.orderId }}</td>
              <td>{{ order.tableNumber }}</td>
              <td>{{ order.username }}</td>
              <td>¥{{ order.totalAmount }}</td>
              <td><span class="badge">{{ order.status }}</span></td>
              <td>{{ isPaid(order) ? '已支付' : '未支付' }}</td>
              <td>{{ order.orderTime }}</td>
              <td>
                <button v-if="isPaid(order)" class="ghost" @click="unpay(order.orderId)">反结账</button>
              </td>
            </tr>
            <tr v-if="filteredTodayOrders.length === 0">
              <td colspan="8">暂无符合条件的订单</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="showMonthlyStats" class="modal-mask" @click.self="showMonthlyStats = false">
      <div class="orders-modal">
        <header>
          <h3>月度统计</h3>
          <button class="ghost" @click="showMonthlyStats = false">关闭</button>
        </header>
        <table>
          <thead><tr><th>月份</th><th>订单数</th><th>营业额</th></tr></thead>
          <tbody>
            <tr v-for="item in months" :key="item.month">
              <td>{{ item.month }}</td>
              <td>{{ item.orderCount }}</td>
              <td>¥{{ item.revenue }}</td>
            </tr>
            <tr v-if="months.length === 0">
              <td colspan="3">暂无月度统计数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  createOrder,
  createRefund,
  createTable,
  dashboard,
  getOrder,
  listCategories,
  listDishes,
  listOrders,
  listTables,
  monthlyRevenue,
  payOrder,
  unpayOrder,
  updateOrderStatus,
  updateTable,
  updateTableStatus
} from '../api'

const data = ref({})
const tables = ref([])
const orders = ref([])
const dishes = ref([])
const categories = ref([])
const activeTable = ref(null)
const pendingOpenTableId = ref(null)
const activeCategoryId = ref(null)
const cart = ref([])
const message = ref('')
const showTodayOrders = ref(false)
const showMonthlyStats = ref(false)
const dishKeyword = ref('')
const refundKeyword = ref('')
const months = ref([])
const user = computed(() => JSON.parse(localStorage.getItem('user') || '{}'))
const orderFilters = reactive({
  keyword: '',
  minAmount: null,
  maxAmount: null,
  startTime: '',
  endTime: '',
  orderState: ''
})
const activeOrders = ref([])
const refundDrafts = reactive({})
const showAddTable = ref(false)
const editingTableId = ref(null)
const newTableForm = reactive({
  tableNumber: '',
  tableName: '',
  area: '',
  capacity: 4
})
const editTableForm = reactive({
  tableName: '',
  area: '',
  capacity: 0
})

const filteredDishes = computed(() => {
  const keyword = dishKeyword.value.toLowerCase()
  return dishes.value.filter((dish) => {
    const matchesCategory = activeCategoryId.value === null || dish.categoryId === activeCategoryId.value
    const haystack = `${dish.dishName || ''} ${dish.categoryName || ''} ${dish.description || ''}`.toLowerCase()
    const matchesKeyword = !keyword || haystack.includes(keyword)
    return matchesCategory && matchesKeyword
  })
})
const cartTotal = computed(() => cart.value.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0))
const activeTotal = computed(() => activeOrders.value.reduce((sum, order) => sum + Number(order.totalAmount || 0), 0))
const todayOrders = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return orders.value.filter((order) => String(order.orderTime || '').startsWith(today))
})
const todayUnpaidCount = computed(() => todayOrders.value.filter((order) => !isPaid(order)).length)
const todayPaidAmount = computed(() => {
  return todayOrders.value.reduce((sum, order) => sum + (isPaid(order) ? Number(order.paidAmount || order.totalAmount || 0) : 0), 0)
})
const todayUnpaidAmount = computed(() => {
  return todayOrders.value.reduce((sum, order) => sum + (!isPaid(order) ? Number(order.totalAmount || 0) : 0), 0)
})
const todayTotalAmount = computed(() => todayPaidAmount.value + todayUnpaidAmount.value)
const filteredTodayOrders = computed(() => {
  return todayOrders.value.filter((order) => {
    const keyword = orderFilters.keyword.toLowerCase()
    const amount = Number(order.totalAmount || 0)
    const time = String(order.orderTime || '').replace(' ', 'T')
    const haystack = `${order.orderId || ''} ${order.tableNumber || ''} ${order.username || ''}`.toLowerCase()
    const matchesKeyword = !keyword || haystack.includes(keyword)
    const matchesMin = orderFilters.minAmount === null || orderFilters.minAmount === '' || amount >= Number(orderFilters.minAmount)
    const matchesMax = orderFilters.maxAmount === null || orderFilters.maxAmount === '' || amount <= Number(orderFilters.maxAmount)
    const matchesStart = !orderFilters.startTime || time >= orderFilters.startTime
    const matchesEnd = !orderFilters.endTime || time <= orderFilters.endTime
    const matchesState = !orderFilters.orderState || order.status === orderFilters.orderState
    return matchesKeyword && matchesMin && matchesMax && matchesStart && matchesEnd && matchesState
  })
})
const orderedDetails = computed(() => {
  const groups = new Map()
  activeOrders.value.forEach((order) => {
    ;(order.details || []).forEach((detail) => {
      const key = String(detail.dishId)
      const existing = groups.get(key)
      const quantity = Number(detail.quantity || 0)
      const subtotal = Number(detail.subtotal || 0)
      const source = { orderId: order.orderId, dishId: detail.dishId, quantity, unitPrice: Number(detail.unitPrice || 0) }
      if (existing) {
        existing.quantity += quantity
        existing.subtotal += subtotal
        existing.sources.push(source)
        if (detail.remark && !existing.remark.includes(detail.remark)) {
          existing.remark = existing.remark ? `${existing.remark}; ${detail.remark}` : detail.remark
        }
      } else {
        groups.set(key, {
          dishId: detail.dishId,
          dishName: detail.dishName,
          quantity,
          subtotal,
          unitPrice: Number(detail.unitPrice || 0),
          remark: detail.remark || '',
          sources: [source]
        })
      }
    })
  })
  return Array.from(groups.values()).map((detail) => {
    const draft = refundDrafts[detail.dishId] || {}
    return {
      ...detail,
      selected: Boolean(draft.selected),
      refundQuantity: draft.refundQuantity || 1,
      refundReason: draft.refundReason || ''
    }
  })
})
const filteredOrderedDetails = computed(() => {
  const keyword = refundKeyword.value.toLowerCase()
  return orderedDetails.value.filter((detail) => !keyword || String(detail.dishName || '').toLowerCase().includes(keyword))
})

function statusText(status) {
  return {
    FREE: '空闲',
    OCCUPIED: '占用',
    RESERVED: '预订',
    CLEANING: '待清扫'
  }[status] || status
}

async function load() {
  const [dashboardData, tableData, orderData, dishData, categoryData, monthData] = await Promise.all([
    dashboard(),
    listTables(),
    listOrders(),
    listDishes(),
    listCategories(),
    monthlyRevenue()
  ])
  data.value = dashboardData
  tables.value = tableData
  orders.value = orderData
  dishes.value = dishData
  categories.value = categoryData
  months.value = monthData
}

function isPaid(order) {
  return ['PAID', 'COMPLETED'].includes(order.status) || Number(order.paidAmount || 0) > 0 || Boolean(order.payNo)
}

function resetOrderFilters() {
  Object.assign(orderFilters, {
    keyword: '',
    minAmount: null,
    maxAmount: null,
    startTime: '',
    endTime: '',
    orderState: ''
  })
}

async function selectTable(table) {
  if (table.status === 'FREE') {
    pendingOpenTableId.value = pendingOpenTableId.value === table.tableId ? null : table.tableId
    activeTable.value = null
    return
  }
  pendingOpenTableId.value = null
  activeTable.value = { ...table }
  message.value = ''
  cart.value = []
  await loadActiveOrders()
}

async function loadActiveOrders() {
  return loadActiveOrdersFrom(orders.value)
}

async function loadActiveOrdersFrom(sourceOrders) {
  if (!activeTable.value || activeTable.value.status === 'FREE') {
    activeOrders.value = []
    return
  }
  const tableOrders = sourceOrders.filter((item) => {
    return item.tableId === activeTable.value.tableId && !['COMPLETED', 'CANCELLED'].includes(item.status)
  })
  const nextOrders = await Promise.all(tableOrders.map((order) => getOrder(order.orderId)))
  activeOrders.value = nextOrders
}

function closeTableModal() {
  activeTable.value = null
  activeOrders.value = []
  cart.value = []
  refundKeyword.value = ''
}

async function openTableFromCard(table) {
  await updateTableStatus(table.tableId, 'OCCUPIED')
  message.value = '开台成功，可以开始点餐。'
  pendingOpenTableId.value = null
  activeTable.value = { ...table, status: 'OCCUPIED' }
  await refreshAfterTableMutation()
  await loadActiveOrders()
}

function addToCart(dish) {
  const exists = cart.value.find((item) => item.dishId === dish.dishId)
  if (exists) increase(exists)
  else cart.value.push({ dishId: dish.dishId, dishName: dish.dishName, price: dish.price, stock: dish.stock, quantity: 1, remark: '' })
}

function increase(item) {
  if (item.quantity < item.stock) item.quantity += 1
}

function decrease(item) {
  if (item.quantity <= 1) cart.value = cart.value.filter((cartItem) => cartItem.dishId !== item.dishId)
  else item.quantity -= 1
}

function removeCartItem(dishId) {
  cart.value = cart.value.filter((item) => item.dishId !== dishId)
}

async function submitOrder() {
  try {
    const order = await createOrder({
      tableId: activeTable.value.tableId,
      userId: user.value.userId,
      items: cart.value.map((item) => ({ dishId: item.dishId, quantity: item.quantity, remark: item.remark }))
    })
    cart.value = []
    message.value = `提交成功，订单号：${order.orderId}`
    await refreshAfterTableMutation()
  } catch (error) {
    message.value = error.message || '提交加菜失败'
  }
}

async function refundDetail(detail) {
  const quantity = Number(detail.refundQuantity || 0)
  if (!quantity || quantity < 1 || quantity > detail.quantity) {
    message.value = '请输入有效的退菜份数。'
    return
  }
  await refundGroupedDetail(detail, quantity)
}

async function refundGroupedDetail(detail, quantity) {
  let remaining = quantity
  for (const source of detail.sources) {
    if (remaining <= 0) break
    const refundQuantity = Math.min(remaining, source.quantity)
    remaining -= refundQuantity
    await createRefund({
      orderId: source.orderId,
      dishId: source.dishId,
      quantity: refundQuantity,
      refundAmount: Number(source.unitPrice || detail.unitPrice || 0) * refundQuantity,
      refundReason: detail.refundReason || '退菜',
      stockAction: 1
    })
  }
  delete refundDrafts[detail.dishId]
  message.value = '退菜成功，已刷新当前桌台菜品。'
  await refreshAfterTableMutation()
}

const selectedRefundDetails = computed(() => filteredOrderedDetails.value.filter((detail) => detail.selected))

async function refundSelected() {
  const selected = [...selectedRefundDetails.value]
  for (const detail of selected) {
    const quantity = Number(detail.refundQuantity || 0)
    if (quantity > 0 && quantity <= detail.quantity) {
      await refundGroupedDetail(detail, quantity)
    }
  }
}

function ensureRefundDraft(detail) {
  if (!refundDrafts[detail.dishId]) {
    refundDrafts[detail.dishId] = { selected: false, refundQuantity: 1, refundReason: '' }
  }
  return refundDrafts[detail.dishId]
}

function updateRefundDraft(detail, field, value) {
  const draft = ensureRefundDraft(detail)
  draft[field] = value
}

async function checkout() {
  if (activeOrders.value.length === 0) return
  for (const order of activeOrders.value) {
    await payOrder(order.orderId, { payMethod: '现金', discountAmount: 0 })
    await updateOrderStatus(order.orderId, 'COMPLETED')
  }
  await updateTableStatus(activeTable.value.tableId, 'FREE')
  message.value = '结账完成，桌位已释放。'
  activeTable.value.status = 'FREE'
  activeOrders.value = []
  await refreshAfterTableMutation()
}

async function addTable() {
  if (!newTableForm.tableNumber) {
    message.value = '请输入桌号（编号）。'
    return
  }
  try {
    await createTable({ ...newTableForm })
    newTableForm.tableNumber = ''
    newTableForm.tableName = ''
    newTableForm.area = ''
    newTableForm.capacity = 4
    showAddTable.value = false
    message.value = '桌台添加成功。'
    await refreshAfterTableMutation()
  } catch (error) {
    message.value = error.message || '添加桌台失败'
  }
}

function startEditTable(table) {
  editingTableId.value = table.tableId
  editTableForm.tableName = table.tableName || ''
  editTableForm.area = table.area || ''
  editTableForm.capacity = table.capacity || 4
}

async function saveEditTable(table) {
  try {
    await updateTable(table.tableId, {
      tableName: editTableForm.tableName || null,
      area: editTableForm.area || null,
      capacity: editTableForm.capacity
    })
    editingTableId.value = null
    message.value = '桌台信息已更新。'
    await refreshAfterTableMutation()
  } catch (error) {
    message.value = error.message || '更新桌台失败'
  }
}

function cancelEditTable() {
  editingTableId.value = null
}

async function unpay(orderId) {
  try {
    await unpayOrder(orderId)
    message.value = '反结账成功，订单已恢复为待支付状态。'
    await refreshAfterTableMutation()
  } catch (error) {
    message.value = error.message || '反结账失败'
  }
}

async function closeTableWithoutCheckout() {
  await updateTableStatus(activeTable.value.tableId, 'FREE')
  message.value = activeOrders.value.length > 0 ? '已关台，未结账订单仍保留。' : '已关台。'
  activeTable.value.status = 'FREE'
  await refreshAfterTableMutation()
  closeTableModal()
}

async function refreshAfterTableMutation() {
  const [dashboardData, orderData, dishData, tableData] = await Promise.all([
    dashboard(),
    listOrders(),
    listDishes(),
    listTables()
  ])
  data.value = dashboardData
  orders.value = orderData
  dishes.value = dishData
  tables.value = tableData
  if (activeTable.value) {
    const latestTable = tableData.find((table) => table.tableId === activeTable.value.tableId)
    if (latestTable) {
      activeTable.value = { ...activeTable.value, ...latestTable }
    }
  }
  await loadActiveOrdersFrom(orderData)
}

function handleDocumentClick(event) {
  if (!pendingOpenTableId.value) return
  if (!event.target.closest('.floor-table')) {
    pendingOpenTableId.value = null
  }
}

onMounted(() => {
  load()
  document.addEventListener('click', handleDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
})
</script>
