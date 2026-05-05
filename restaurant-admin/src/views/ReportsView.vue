<template>
  <section>
    <h2>菜品统计</h2>
    <div class="report-grid">
      <article>
        <div class="report-card-head">
          <h3>热销菜品</h3>
          <div class="report-actions">
            <select v-model="hotPeriod" @change="loadHotDishes">
              <option value="">全部</option>
              <option value="day">今日</option>
              <option value="week">本周</option>
              <option value="month">本月</option>
            </select>
            <button class="ghost" @click="showHot = !showHot">{{ showHot ? '收起详情' : '查看详情' }}</button>
          </div>
        </div>
        <table v-if="showHot">
          <thead><tr><th>菜品</th><th>分类</th><th>销量</th><th>销售额</th></tr></thead>
          <tbody>
            <tr v-for="item in dishes" :key="item.dishName">
              <td>{{ item.dishName }}</td>
              <td>{{ item.categoryName }}</td>
              <td>{{ item.sales }}份</td>
              <td>¥{{ item.amount }}</td>
            </tr>
          </tbody>
        </table>
        <p v-else class="summary-text">当前共统计 {{ dishes.length }} 个热销菜品。</p>
      </article>
      <article>
        <div class="report-card-head">
          <h3>低库存菜品</h3>
          <button class="ghost" @click="showLowStock = !showLowStock">{{ showLowStock ? '收起详情' : '查看详情' }}</button>
        </div>
        <table v-if="showLowStock">
          <thead><tr><th>菜品</th><th>分类</th><th>库存</th><th>价格</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="item in lowStocks" :key="item.dishId">
              <td>{{ item.dishName }}</td>
              <td>{{ item.categoryName }}</td>
              <td>{{ item.stock }}</td>
              <td>¥{{ item.price }}</td>
              <td>{{ item.available ? '上架' : '下架' }}</td>
            </tr>
          </tbody>
        </table>
        <p v-else class="summary-text">当前共有 {{ lowStocks.length }} 个低库存菜品。</p>
      </article>
      <article>
        <div class="report-card-head">
          <h3>评价管理</h3>
          <select v-model.number="ratingFilter">
            <option :value="0">全部评分</option>
            <option v-for="n in 5" :key="n" :value="n">{{ n }}星</option>
          </select>
        </div>
        <table>
          <thead><tr><th>评分</th><th>顾客</th><th>订单</th><th>金额</th><th>评价</th></tr></thead>
          <tbody>
            <tr v-for="item in filteredReviews" :key="item.reviewId" :class="{ 'bad-review-row': item.rating <= 2 }">
              <td><span :class="'rating-badge rating-' + item.rating">{{ item.rating }}星</span></td>
              <td>{{ item.username }}</td>
              <td>#{{ item.orderId }}</td>
              <td>¥{{ item.totalAmount }}</td>
              <td>{{ item.comment || '无文字评价' }}</td>
            </tr>
            <tr v-if="filteredReviews.length === 0">
              <td colspan="5">暂无符合条件的评价</td>
            </tr>
          </tbody>
        </table>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { hotDishes, listReviews, lowStockDishes } from '../api'

const dishes = ref([])
const lowStocks = ref([])
const reviews = ref([])
const showHot = ref(true)
const showLowStock = ref(true)
const ratingFilter = ref(0)
const hotPeriod = ref('month')
const filteredReviews = computed(() => {
  if (!ratingFilter.value) return reviews.value
  return reviews.value.filter(item => Number(item.rating) === Number(ratingFilter.value))
})

async function loadHotDishes() {
  dishes.value = await hotDishes({ period: hotPeriod.value })
}

onMounted(async () => {
  const results = await Promise.allSettled([
    hotDishes({ period: hotPeriod.value }),
    lowStockDishes({ threshold: 20 }),
    listReviews()
  ])
  dishes.value = results[0].status === 'fulfilled' ? results[0].value : []
  lowStocks.value = results[1].status === 'fulfilled' ? results[1].value : []
  reviews.value = results[2].status === 'fulfilled' ? results[2].value : []
})
</script>
