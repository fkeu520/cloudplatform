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
          <template v-if="menuItems.length > 0">
            <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
              <i :class="item.icon" style="margin-right:6px;width:16px;text-align:center" />
              <span>{{ item.label }}</span>
            </el-menu-item>
          </template>
          <div v-else class="menu-empty">暂无菜单权限</div>
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
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { logout as logoutApi } from '@/api/auth'
import { getMenuTree } from '@/api/menu'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// OPA2-4 + V40+#36.2: 数据驱动菜单, 后端按 menu_category='ops-admin' 过滤后返回
interface MenuItem { index: string; icon: string; label: string }
const menuItems = ref<MenuItem[]>([])

onMounted(async () => {
  try {
    const res: any = await getMenuTree()
    const menus = res.data || []
    // 扁平化后端菜单树为平铺菜单项 (仅保留 leaf-level 且有 path 的)
    const flat: MenuItem[] = []
    function walk(items: any[]) {
      for (const m of items) {
        if (m.path && m.path !== '/' && (!m.children || m.children.length === 0)) {
          flat.push({ index: m.path, icon: m.icon ? `fas fa-${m.icon}` : 'fas fa-circle', label: m.name })
        }
        if (m.children?.length) walk(m.children)
      }
    }
    walk(menus)
    menuItems.value = flat
  } catch {
    // 后端菜单接口异常 → 留空, 由 el-menu 显示 "暂无菜单权限"
    menuItems.value = []
  }
})

async function handleLogout() {
  try {
    await logoutApi()
  } catch {
    // 后端登出接口不可用时，仍然清除本地状态
  }
  userStore.logout()
  router.push('/login')
}

function openKafkaUI() {
  window.open('http://localhost:8089', '_blank')
}
</script>

<style scoped>
.menu-empty {
  color: #bfcbd9;
  text-align: center;
  padding: 20px;
  font-size: 13px;
}
</style>
