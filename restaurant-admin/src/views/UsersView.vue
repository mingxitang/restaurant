<template>
  <section>
    <div class="section-head">
      <h2>身份管理</h2>
    </div>
    <form class="editor" @submit.prevent="save">
      <input v-model="form.username" placeholder="姓名" />
      <input v-model="form.phone" placeholder="手机号" />
      <input v-model="form.password" placeholder="密码" />
      <select v-model.number="form.roleId">
        <option :value="1">管理员</option>
        <option :value="2">服务员</option>
        <option :value="3">厨师</option>
        <option :value="4">顾客</option>
      </select>
      <label class="inline"><input v-model="form.enabled" type="checkbox" /> 启用</label>
      <button>保存身份</button>
    </form>
    <table>
      <thead>
        <tr><th>ID</th><th>姓名</th><th>手机号</th><th>角色</th><th>状态</th></tr>
      </thead>
      <tbody>
        <tr v-for="item in rows" :key="item.userId" class="editable-row" @click="edit(item)">
          <td>{{ item.userId }}</td>
          <td>{{ item.username }}</td>
          <td>{{ item.phone }}</td>
          <td>{{ item.roleName }}</td>
          <td>{{ item.enabled ? '启用' : '禁用' }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { createUser, listUsers, updateUser } from '../api'

const rows = ref([])
const form = reactive({ userId: null, username: '', phone: '', password: '', roleId: 2, enabled: true })

async function load() {
  rows.value = await listUsers()
}

function edit(row) {
  Object.assign(form, row, { password: '' })
}

async function save() {
  if (form.userId) await updateUser(form.userId, form)
  else await createUser(form)
  Object.assign(form, { userId: null, username: '', phone: '', password: '', roleId: 2, enabled: true })
  load()
}

onMounted(load)
</script>
