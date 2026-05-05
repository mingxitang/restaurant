<template>
  <section class="frontdesk">
    <h2>运营首页<button class="ghost" style="float:right" @click="toggleEditMode">{{ showEditMode ? '完成' : '编辑' }}</button></h2>
    <p v-if="message && !activeTable" class="order-message">{{ message }}</p>
    <div class="stats">
      <article v-if="isAdmin" class="clickable-stat" @click="showTodayOrders = true">
        <span>今日订单</span>
        <strong>{{ data.todayOrders || 0 }}</strong>
        <small v-if="todayUnpaidCount > 0">未结 {{ todayUnpaidCount }}</small>
      </article>
      <article v-if="isAdmin" class="clickable-stat revenue-card" @click="showMonthlyStats = true">
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
      </div>
    </div>

    <div class="floor-grid">
      <div
        v-for="table in tables"
        :key="table.tableId"
        class="floor-table"
        :class="table.status.toLowerCase()"
        @click.stop="selectTable(table)"
      >
        <template v-if="showEditMode">
          <input v-model.trim="editForm[table.tableId].tableName" :placeholder="table.tableNumber" @click.stop />
          <input v-model.trim="editForm[table.tableId].area" placeholder="区域" @click.stop />
          <input v-model.number="editForm[table.tableId].capacity" type="number" min="1" max="20" @click.stop />
          <div class="edit-actions">
            <button class="ghost" @click.stop="saveEditTable(table)">保存</button>
            <button class="danger" @click.stop="deleteTableById(table.tableId)">删除</button>
          </div>
        </template>
        <template v-else-if="pendingOpenTableId === table.tableId && !isChef">
          <button class="open-inline open-only" @click.stop="openTableFromCard(table)">开台</button>
        </template>
        <template v-else>
          <strong>{{ table.tableName || table.tableNumber }}</strong>
          <span>{{ table.area || '大厅' }} · {{ table.capacity }}人</span>
          <em>{{ statusText(table.status) }}</em>
        </template>
      </div>
      <div v-if="showEditMode" class="floor-table" style="border-style:dashed" @click.stop>
        <input v-model.trim="newTableForm.tableNumber" placeholder="桌号（编号）" />
        <input v-model.trim="newTableForm.tableName" placeholder="桌台名称（可选）" />
        <input v-model.trim="newTableForm.area" placeholder="区域" />
        <input v-model.number="newTableForm.capacity" type="number" min="1" max="20" placeholder="人数" />
        <button @click="addTable">添加</button>
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
          <div v-if="!isChef" class="order-menu">
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
            <div v-if="!isChef" class="checkout-strip">
              <div>
                <span>桌台应收</span>
                <strong>¥{{ activeTotal.toFixed(2) }}</strong>
              </div>
              <div class="checkout-actions">
                <button class="ghost" @click="closeTableWithoutCheckout">关台</button>
                <button :disabled="activeOrders.length === 0" @click="checkout">结账</button>
              </div>
            </div>

            <div v-if="!isChef && activeOrders.length > 0" class="cart-section transfer-section">
              <div class="cart-section-head">
                <h4>换桌 / 并桌</h4>
                <button :disabled="!transferTargetId" @click="transferActiveTable">确认</button>
              </div>
              <select v-model.number="transferTargetId">
                <option :value="null">选择目标桌台</option>
                <option v-for="table in transferTargetOptions" :key="table.tableId" :value="table.tableId">
                  {{ table.tableName || table.tableNumber }} · {{ table.area || '大厅' }} · {{ statusText(table.status) }}
                </option>
              </select>
              <p class="compact-empty">目标为空闲桌时换桌，目标有未结订单时并桌合并订单。</p>
            </div>

            <div v-if="!isChef" class="cart-section">
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
                  <button v-if="!isChef" class="danger" :disabled="selectedRefundDetails.length === 0" @click="refundSelected">批量退菜</button>
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
                    <input v-if="!isChef" :checked="detail.selected" type="checkbox" @change="updateRefundDraft(detail, 'selected', $event.target.checked)" />
                    <strong>{{ detail.dishName }}</strong>
                  </label>
                  <span>{{ detail.quantity }}份 · ¥{{ detail.subtotal.toFixed(2) }}</span>
                  <small v-if="detail.remark">{{ detail.remark }}</small>
                </div>
                <div v-if="!isChef" class="line-controls">
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
          <select v-model="orderFilters.year"><option :value="null">全部年份</option><option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</option></select>
          <select v-model="orderFilters.month"><option :value="null">全部月份</option><option v-for="m in 12" :key="m" :value="m">{{ m }}月</option></select>
          <select v-model="orderFilters.day"><option :value="null">全部日期</option><option v-for="d in 31" :key="d" :value="d">{{ d }}日</option></select>
          <select v-model="orderFilters.timePeriod">
            <option value="">全天</option>
            <option value="morning">早餐 06-10</option>
            <option value="noon">午餐 10-14</option>
            <option value="afternoon">下午茶 14-17</option>
            <option value="evening">晚餐 17-21</option>
            <option value="night">夜宵 21-06</option>
          </select>
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
  deleteTable,
  dashboard,
  getOrder,
  listCategories,
  listDishes,
  listOrders,
  listTables,
  monthlyRevenue,
  payOrder,
  transferTable,
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
const transferTargetId = ref(null)
const months = ref([])
const user = computed(() => JSON.parse(localStorage.getItem('user') || '{}'))
const isChef = computed(() => user.value?.roleName === '厨师')
const isAdmin = computed(() => user.value?.roleName === '管理员')
const orderFilters = reactive({
  keyword: '',
  minAmount: null,
  maxAmount: null,
  year: null,
  month: null,
  day: null,
  timePeriod: '',
  orderState: ''
})
const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 11 }, (_, i) => currentYear - 5 + i)
const activeOrders = ref([])
const refundDrafts = reactive({})
const showEditMode = ref(false)
const editForm = reactive({})
const newTableForm = reactive({
  tableNumber: '',
  tableName: '',
  area: '',
  capacity: 4
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
const hasDateFilter = computed(() => {
  return orderFilters.year !== null || orderFilters.month !== null || orderFilters.day !== null || orderFilters.timePeriod !== ''
})
const periodRanges = {
  morning: [6, 10],
  noon: [10, 14],
  afternoon: [14, 17],
  evening: [17, 21],
  night: [21, 24],
  night2: [0, 6]
}
const filteredTodayOrders = computed(() => {
  const baseOrders = hasDateFilter.value ? orders.value : todayOrders.value
  return baseOrders.filter((order) => {
    const keyword = orderFilters.keyword.toLowerCase()
    const amount = Number(order.totalAmount || 0)
    const haystack = `${order.orderId || ''} ${order.tableNumber || ''} ${order.username || ''}`.toLowerCase()
    const matchesKeyword = !keyword || haystack.includes(keyword)
    const matchesMin = orderFilters.minAmount === null || orderFilters.minAmount === '' || amount >= Number(orderFilters.minAmount)
    const matchesMax = orderFilters.maxAmount === null || orderFilters.maxAmount === '' || amount <= Number(orderFilters.maxAmount)
    const matchesState = !orderFilters.orderState || order.status === orderFilters.orderState
    if (!hasDateFilter.value) return matchesKeyword && matchesMin && matchesMax && matchesState
    let matchesDate = true
    const timeStr = String(order.orderTime || '')
    const datePart = timeStr.slice(0, 10)
    if (orderFilters.year !== null) {
      matchesDate = matchesDate && datePart.slice(0, 4) === String(orderFilters.year)
    }
    if (orderFilters.month !== null) {
      matchesDate = matchesDate && datePart.slice(5, 7) === String(orderFilters.month).padStart(2, '0')
    }
    if (orderFilters.day !== null) {
      matchesDate = matchesDate && datePart.slice(8, 10) === String(orderFilters.day).padStart(2, '0')
    }
    let matchesPeriod = true
    if (orderFilters.timePeriod) {
      const timePart = timeStr.slice(11, 13)
      const hour = parseInt(timePart, 10)
      if (isNaN(hour)) matchesPeriod = false
      else if (orderFilters.timePeriod === 'night') {
        matchesPeriod = (hour >= 21 && hour <= 23) || (hour >= 0 && hour < 6)
      } else {
        const [from, to] = periodRanges[orderFilters.timePeriod]
        matchesPeriod = hour >= from && hour < to
      }
    }
    return matchesKeyword && matchesMin && matchesMax && matchesDate && matchesPeriod && matchesState
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
const transferTargetOptions = computed(() => {
  if (!activeTable.value) return []
  return tables.value.filter((table) => {
    return table.tableId !== activeTable.value.tableId && ['FREE', 'OCCUPIED'].includes(table.status)
  })
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
  const results = await Promise.allSettled([
    dashboard(),
    listTables(),
    listOrders(),
    listDishes(),
    listCategories(),
    monthlyRevenue()
  ])
  data.value = results[0].status === 'fulfilled' ? results[0].value : {}
  tables.value = results[1].status === 'fulfilled' ? results[1].value : []
  orders.value = results[2].status === 'fulfilled' ? results[2].value : []
  dishes.value = results[3].status === 'fulfilled' ? results[3].value : []
  categories.value = results[4].status === 'fulfilled' ? results[4].value : []
  months.value = results[5].status === 'fulfilled' ? results[5].value : []
}

function isPaid(order) {
  return ['PAID', 'COMPLETED'].includes(order.status) || Number(order.paidAmount || 0) > 0 || Boolean(order.payNo)
}

function resetOrderFilters() {
  Object.assign(orderFilters, {
    keyword: '',
    minAmount: null,
    maxAmount: null,
    year: null,
    month: null,
    day: null,
    timePeriod: '',
    orderState: ''
  })
}

async function selectTable(table) {
  if (table.status === 'FREE') {
    if (isChef.value) return
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
  transferTargetId.value = null
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

function toggleEditMode() {
  showEditMode.value = !showEditMode.value
  if (showEditMode.value) {
    tables.value.forEach(table => {
      editForm[table.tableId] = {
        tableName: table.tableName || '',
        area: table.area || '',
        capacity: table.capacity || 4
      }
    })
  } else {
    Object.keys(editForm).forEach(key => delete editForm[key])
  }
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
    message.value = '桌台添加成功。'
    await refreshAfterTableMutation()
  } catch (error) {
    message.value = error.message || '添加桌台失败'
  }
}

async function saveEditTable(table) {
  const form = editForm[table.tableId]
  if (!form) return
  try {
    await updateTable(table.tableId, {
      tableName: form.tableName || null,
      area: form.area || null,
      capacity: form.capacity
    })
    message.value = '桌台信息已更新。'
    await refreshAfterTableMutation()
  } catch (error) {
    message.value = error.message || '更新桌台失败'
  }
}

async function deleteTableById(tableId) {
  if (!confirm('确定要删除该桌台吗？')) return
  try {
    await deleteTable(tableId)
    delete editForm[tableId]
    message.value = '桌台已删除。'
    await refreshAfterTableMutation()
  } catch (error) {
    message.value = error.message || '删除桌台失败'
  }
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

async function transferActiveTable() {
  if (!activeTable.value || !transferTargetId.value) return
  const target = tables.value.find((table) => table.tableId === transferTargetId.value)
  try {
    await transferTable(activeTable.value.tableId, transferTargetId.value)
    message.value = target?.status === 'OCCUPIED' ? '并桌成功，订单已合并到目标桌。' : '换桌成功，订单已转移到目标桌。'
    transferTargetId.value = null
    await refreshAfterTableMutation()
    closeTableModal()
  } catch (error) {
    message.value = error.message || '换桌/并桌失败'
  }
}

async function refreshAfterTableMutation() {
  const results = await Promise.allSettled([
    dashboard(),
    listOrders(),
    listDishes(),
    listTables()
  ])
  data.value = results[0].status === 'fulfilled' ? results[0].value : {}
  orders.value = results[1].status === 'fulfilled' ? results[1].value : []
  dishes.value = results[2].status === 'fulfilled' ? results[2].value : []
  tables.value = results[3].status === 'fulfilled' ? results[3].value : []
  if (activeTable.value) {
    const latestTable = (results[3].status === 'fulfilled' ? results[3].value : []).find((table) => table.tableId === activeTable.value.tableId)
    if (latestTable) {
      activeTable.value = { ...activeTable.value, ...latestTable }
    }
  }
  await loadActiveOrdersFrom(results[1].status === 'fulfilled' ? results[1].value : [])
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
