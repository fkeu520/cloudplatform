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
            <el-icon><UserFilled /></el-icon>
            <span>租户管理</span>
          </el-menu-item>
          <el-menu-item index="/storage">
            <el-icon><FolderOpened /></el-icon>
            <span>对象存储</span>
          </el-menu-item>
          <el-menu-item index="/gateway">
            <el-icon><Connection /></el-icon>
            <span>服务网关</span>
          </el-menu-item>
          <el-menu-item index="/audit">
            <el-icon><List /></el-icon>
            <span>日志审计</span>
          </el-menu-item>
          <el-menu-item index="/message/record">
            <el-icon><Bell /></el-icon>
            <span>消息记录</span>
          </el-menu-item>
          <el-menu-item index="/kafka" @click="openKafkaUI">
            <el-icon><Message /></el-icon>
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
import { useRouter, useRoute } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'

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
