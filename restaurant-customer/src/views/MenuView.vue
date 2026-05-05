<template>
  <div class="menu-page">
    <header class="app-header">
      <h2>{{ store.tableNumber }}</h2>
    </header>
    <div class="search-bar">
      <input v-model.trim="keyword" placeholder="搜索菜品..." />
    </div>
    <div class="category-tabs">
      <button :class="{ active: activeCat === null }" @click="activeCat = null">全部</button>
      <button
        v-for="cat in categories"
        :key="cat.categoryId"
        :class="{ active: activeCat === cat.categoryId }"
        @click="activeCat = cat.categoryId"
      >{{ cat.categoryName }}</button>
    </div>
    <div class="dish-list">
      <div
        v-for="dish in filteredDishes"
        :key="dish.dishId"
        class="dish-card"
        :class="{ disabled: !dish.available || dish.stock <= 0 }"
      >
        <div class="dish-thumb">
          <img v-if="dish.image" :src="dish.image" :alt="dish.dishName" />
          <span v-else>{{ dish.dishName?.slice(0, 1) || '菜' }}</span>
        </div>
        <div class="info">
          <h4>{{ dish.dishName }}</h4>
          <p v-if="dish.description">{{ dish.description }}</p>
          <div class="price">&yen;{{ dish.price.toFixed(2) }}</div>
          <div class="stock" v-if="dish.stock <= 5 && dish.stock > 0">仅剩 {{ dish.stock }} 份</div>
          <div class="stock" v-else-if="dish.stock <= 0">已售罄</div>
        </div>
        <button
          class="add-btn"
          v-if="dish.available && dish.stock > 0"
          @click="store.addToCart(dish); showToast(dish.dishName + ' 已加入')"
        >+</button>
      </div>
      <p v-if="!filteredDishes.length" style="text-align:center;color:#999;padding:40px">暂无菜品</p>
    </div>

    <div v-if="cartOpen && store.cartCount > 0" class="cart-drawer">
      <header>
        <strong>未下单菜品</strong>
        <div class="cart-drawer-actions">
          <button class="ghost" @click="store.clearCart(); cartOpen = false">清空</button>
          <button class="ghost" @click="cartOpen = false">收起</button>
        </div>
      </header>
      <div class="cart-drawer-item" v-for="item in store.cart" :key="item.dishId">
        <div>
          <strong>{{ item.dishName }}</strong>
          <span>&yen;{{ item.price.toFixed(2) }} / 份</span>
          <input
            :value="item.remark"
            placeholder="备注：少辣、不要葱、打包等"
            @input="store.updateCartRemark(item.dishId, $event.target.value)"
          />
        </div>
        <div class="mini-qty">
          <button @click="store.updateCartQty(item.dishId, item.quantity - 1)">-</button>
          <span>{{ item.quantity }}</span>
          <button @click="store.updateCartQty(item.dishId, item.quantity + 1)">+</button>
        </div>
      </div>
    </div>

    <div class="cart-bar" v-if="store.cartCount > 0">
      <div class="cart-click-zone" @click="cartOpen = !cartOpen">
        <div class="cart-icon">
          <span style="font-size:24px">🛒</span>
          <span class="badge">{{ store.cartCount }}</span>
        </div>
        <div>
          <strong>&yen;{{ store.cartTotal.toFixed(2) }}</strong>
          <span>{{ cartOpen ? '点击收起' : '查看未下单菜品' }}</span>
        </div>
      </div>
      <button :disabled="submitting" @click="submitOrder">{{ submitting ? '下单中...' : '下单' }}</button>
    </div>

    <div class="cart-bar settle-bar" v-else-if="hasCurrentOrder">
      <div>
        <strong>已下单</strong>
        <span>没有新加菜，可直接支付</span>
      </div>
      <button @click="goPay">去支付</button>
    </div>

    <p v-if="error" class="error floating-error">{{ error }}</p>
    <div class="toast" v-if="toast">{{ toast }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMenu, placeOrder } from '../api'
import { store } from '../store'

const router = useRouter()
const categories = ref([])
const dishes = ref([])
const activeCat = ref(null)
const keyword = ref('')
const toast = ref('')
const submitting = ref(false)
const error = ref('')
const cartOpen = ref(false)
const hasCurrentOrder = computed(() => Boolean(store.currentOrder?.orderId))

const filteredDishes = computed(() => {
  let list = dishes.value
  if (activeCat.value) {
    list = list.filter(d => d.categoryId === activeCat.value)
  }
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(d =>
      d.dishName.toLowerCase().includes(kw) ||
      (d.description && d.description.toLowerCase().includes(kw))
    )
  }
  return list
})

function showToast(msg) {
  toast.value = msg
  setTimeout(() => { toast.value = '' }, 1500)
}

function goPay() {
  router.push('/pay')
}

async function submitOrder() {
  if (!store.tableId) return router.push('/')
  submitting.value = true
  error.value = ''
  try {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    const data = await placeOrder({
      tableId: store.tableId,
      userId: user?.userId,
      items: store.cart.map(item => ({
        dishId: item.dishId,
        quantity: item.quantity,
        remark: item.remark
      }))
    })
    store.currentOrder = data
    store.clearCart()
    cartOpen.value = false
    router.push('/order')
  } catch (err) {
    error.value = err.message || '下单失败'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (!store.tableId) {
    router.push('/')
    return
  }
  const data = await getMenu()
  categories.value = data.categories || []
  dishes.value = data.dishes || []
})
</script>
