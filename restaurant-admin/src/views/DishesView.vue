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
      <button>保存菜品</button>
    </form>
    <table>
      <thead>
        <tr><th>菜品</th><th>分类</th><th>价格</th><th>库存</th><th>状态</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="item in rows" :key="item.dishId" class="editable-row" @click="edit(item)">
          <td>{{ item.dishName }}</td>
          <td>{{ item.categoryName }}</td>
          <td>¥{{ item.price }}</td>
          <td>{{ item.stock }}</td>
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
import { onMounted, reactive, ref } from 'vue'
import { createDish, deleteDish, listCategories, listDishes, updateDish } from '../api'

const rows = ref([])
const categories = ref([])
const keyword = ref('')
const form = reactive({ dishId: null, dishName: '', price: null, stock: 0, categoryId: null, available: true })

async function load() {
  rows.value = await listDishes({ keyword: keyword.value })
}

function edit(row) {
  Object.assign(form, row)
}

async function save() {
  if (form.dishId) await updateDish(form.dishId, form)
  else await createDish(form)
  Object.assign(form, { dishId: null, dishName: '', price: null, stock: 0, categoryId: null, available: true })
  load()
}

async function remove(id) {
  await deleteDish(id)
  load()
}

onMounted(async () => {
  categories.value = await listCategories()
  load()
})
</script>
