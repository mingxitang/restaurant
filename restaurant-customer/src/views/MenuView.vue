<template>
  <div class="menu-page">
    <header class="app-header">
      <h2>{{ store.tableNumber }}</h2>
      <button @click="goBack">换桌</button>
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

    <div class="cart-bar" v-if="store.cartCount > 0">
      <div class="cart-icon">
        <span style="font-size:24px">🛒</span>
        <span class="badge">{{ store.cartCount }}</span>
      </div>
      <div class="total">&yen;{{ store.cartTotal.toFixed(2) }}</div>
      <button @click="router.push('/cart')">去结算</button>
    </div>

    <div class="toast" v-if="toast">{{ toast }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMenu } from '../api'
import { store } from '../store'

const router = useRouter()
const categories = ref([])
const dishes = ref([])
const activeCat = ref(null)
const keyword = ref('')
const toast = ref('')

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

function goBack() {
  store.tableId = null
  store.tableNumber = ''
  router.push('/')
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
