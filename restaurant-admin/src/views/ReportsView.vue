<template>
  <section>
    <h2>菜品统计</h2>
    <div class="report-grid">
      <article>
        <div class="report-card-head">
          <h3>热销菜品</h3>
          <button class="ghost" @click="showHot = !showHot">{{ showHot ? '收起详情' : '查看详情' }}</button>
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
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { hotDishes, lowStockDishes } from '../api'

const dishes = ref([])
const lowStocks = ref([])
const showHot = ref(true)
const showLowStock = ref(true)

onMounted(async () => {
  const [hotData, lowStockData] = await Promise.all([
    hotDishes(),
    lowStockDishes({ threshold: 20 })
  ])
  dishes.value = hotData
  lowStocks.value = lowStockData
})
</script>
