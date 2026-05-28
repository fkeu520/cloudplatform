<template>
  <el-container style="height: 100vh">
    <el-header style="background: #409eff; color: white; display: flex; align-items: center; padding: 0 20px">
      <h2 style="margin: 0; font-size: 18px">云枢运营管理</h2>
      <div style="flex: 1" />
      <el-button text style="color: white" @click="handleLogout">退出登录</el-button>
    </el-header>
    <el-container>
      <el-aside width="200px" style="background: #304156">
        <el-menu
          :default-active="route.path"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409eff"
          router
        >
          <el-menu-item index="/tenant">
            <i class="fas fa-building" style="margin-right:6px;width:16px;text-align:center" />
            <span>租户管理</span>
          </el-menu-item>
          <el-menu-item index="/storage">
            <i class="fas fa-database" style="margin-right:6px;width:16px;text-align:center" />
            <span>对象存储</span>
          </el-menu-item>
          <el-menu-item index="/gateway">
            <i class="fas fa-plug" style="margin-right:6px;width:16px;text-align:center" />
            <span>服务网关</span>
          </el-menu-item>
          <el-menu-item index="/audit">
            <i class="fas fa-clipboard-list" style="margin-right:6px;width:16px;text-align:center" />
            <span>日志审计</span>
          </el-menu-item>
          <el-menu-item index="/ops-user">
            <i class="fas fa-users-cog" style="margin-right:6px;width:16px;text-align:center" />
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/message/record">
            <i class="fas fa-bell" style="margin-right:6px;width:16px;text-align:center" />
            <span>消息记录</span>
          </el-menu-item>
          <el-menu-item index="/monitor">
            <i class="fas fa-heartbeat" style="margin-right:6px;width:16px;text-align:center" />
            <span>系统监控</span>
          </el-menu-item>
          <el-menu-item index="/kafka" @click="openKafkaUI">
            <i class="fas fa-envelope" style="margin-right:6px;width:16px;text-align:center" />
            <span>消息队列</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import '@fortawesome/fontawesome-free/css/all.min.css'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

function handleLogout() {
  localStorage.removeItem('token')
  router.push('/login')
}

function openKafkaUI() {
  window.open('http://localhost:8089', '_blank')
}
</script>
