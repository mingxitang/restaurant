<template>
  <section>
    <div class="section-head">
      <h2>分类管理</h2>
      <form @submit.prevent="save">
        <input v-model="form.categoryName" placeholder="分类名称" />
        <button>保存</button>
      </form>
    </div>
    <table>
      <thead>
        <tr><th>ID</th><th>分类名称</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="item in rows" :key="item.categoryId" class="editable-row" @click="edit(item)">
          <td>{{ item.categoryId }}</td>
          <td>{{ item.categoryName }}</td>
          <td>
            <button class="danger" @click.stop="remove(item.categoryId)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { createCategory, deleteCategory, listCategories, updateCategory } from '../api'

const rows = ref([])
const form = reactive({ categoryId: null, categoryName: '' })

async function load() {
  rows.value = await listCategories()
}

function edit(row) {
  Object.assign(form, row)
}

async function save() {
  if (form.categoryId) await updateCategory(form.categoryId, form)
  else await createCategory(form)
  Object.assign(form, { categoryId: null, categoryName: '' })
  load()
}

async function remove(id) {
  await deleteCategory(id)
  load()
}

onMounted(load)
</script>
