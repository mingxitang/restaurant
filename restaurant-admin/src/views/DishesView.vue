<template>
  <section>
    <div class="section-head">
      <h2>菜品管理</h2>
      <div>
        <input v-model="keyword" placeholder="搜索菜品" @keyup.enter="load" />
        <button @click="load">查询</button>
      </div>
    </div>
    <form class="editor" @submit.prevent="save">
      <input v-model="form.dishName" placeholder="菜品名称" />
      <input v-model.number="form.price" type="number" step="0.01" placeholder="价格" />
      <input v-model.number="form.stock" type="number" placeholder="库存" />
      <select v-model.number="form.categoryId">
        <option :value="null">选择分类</option>
        <option v-for="category in categories" :key="category.categoryId" :value="category.categoryId">
          {{ category.categoryName }}
        </option>
      </select>
      <label class="inline"><input v-model="form.available" type="checkbox" /> 上架</label>
      <label class="inline image-upload">
        <input type="file" accept="image/*" @change="uploadImage" />
        上传图片
      </label>
      <img v-if="form.image" class="dish-image-preview" :src="imageUrl(form.image)" alt="菜品图片" />
      <button>保存菜品</button>
    </form>
    <p class="warning-summary" v-if="lowStockCount > 0">
      当前有 {{ lowStockCount }} 个菜品库存不足，请及时补货或下架。
    </p>
    <div class="batch-bar" v-if="selectedIds.length > 0">
      <span>已选择 {{ selectedIds.length }} 个菜品</span>
      <button class="ghost" :disabled="batching" @click="batchSetAvailable(true)">批量上架</button>
      <button class="ghost" :disabled="batching" @click="batchSetAvailable(false)">批量下架</button>
      <button class="ghost" @click="selectedIds = []">取消选择</button>
    </div>
    <table>
      <thead>
        <tr>
          <th><input type="checkbox" :checked="allSelected" @change="toggleAll($event.target.checked)" /></th>
          <th>图片</th><th>菜品</th><th>分类</th><th>价格</th><th>库存</th><th>状态</th><th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in rows" :key="item.dishId" class="editable-row" :class="{ 'low-stock-row': isLowStock(item) }" @click="edit(item)">
          <td><input type="checkbox" :checked="selectedIds.includes(item.dishId)" @click.stop @change="toggleOne(item.dishId, $event.target.checked)" /></td>
          <td>
            <img v-if="item.image" class="dish-thumb-small" :src="imageUrl(item.image)" alt="菜品图片" />
            <span v-else class="dish-thumb-empty">无图</span>
          </td>
          <td>{{ item.dishName }}</td>
          <td>{{ item.categoryName }}</td>
          <td>¥{{ item.price }}</td>
          <td>
            <span :class="stockClass(item)">{{ item.stock }}</span>
          </td>
          <td>{{ item.available ? '上架' : '下架' }}</td>
          <td>
            <button class="danger" @click.stop="remove(item.dishId)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { createDish, deleteDish, listCategories, listDishes, updateDish, uploadDishImage } from '../api'

const rows = ref([])
const categories = ref([])
const keyword = ref('')
const selectedIds = ref([])
const batching = ref(false)
const form = reactive({ dishId: null, dishName: '', price: null, stock: 0, image: '', categoryId: null, available: true })
const lowStockCount = computed(() => rows.value.filter(isLowStock).length)
const allSelected = computed(() => rows.value.length > 0 && selectedIds.value.length === rows.value.length)

function imageUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return url.startsWith('/') ? url : `/${url}`
}

function isLowStock(item) {
  return Number(item.stock || 0) <= 10
}

function stockClass(item) {
  if (Number(item.stock || 0) <= 0) return 'stock-badge sold-out'
  if (isLowStock(item)) return 'stock-badge low'
  return 'stock-badge ok'
}

async function load() {
  rows.value = await listDishes({ keyword: keyword.value })
  selectedIds.value = selectedIds.value.filter(id => rows.value.some(item => item.dishId === id))
}

function edit(row) {
  Object.assign(form, row)
}

async function save() {
  if (form.dishId) await updateDish(form.dishId, form)
  else await createDish(form)
  Object.assign(form, { dishId: null, dishName: '', price: null, stock: 0, image: '', categoryId: null, available: true })
  load()
}

async function uploadImage(event) {
  const file = event.target.files?.[0]
  if (!file) return
  const result = await uploadDishImage(file)
  form.image = result.url
  event.target.value = ''
}

async function remove(id) {
  await deleteDish(id)
  load()
}

function toggleOne(id, checked) {
  if (checked && !selectedIds.value.includes(id)) {
    selectedIds.value = [...selectedIds.value, id]
  } else if (!checked) {
    selectedIds.value = selectedIds.value.filter(item => item !== id)
  }
}

function toggleAll(checked) {
  selectedIds.value = checked ? rows.value.map(item => item.dishId) : []
}

async function batchSetAvailable(available) {
  batching.value = true
  try {
    const selected = rows.value.filter(item => selectedIds.value.includes(item.dishId))
    for (const item of selected) {
      await updateDish(item.dishId, { ...item, available })
    }
    selectedIds.value = []
    await load()
  } finally {
    batching.value = false
  }
}

onMounted(async () => {
  categories.value = await listCategories()
  load()
})
</script>
