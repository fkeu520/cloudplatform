<template>
  <div class="dashboard">
    <div class="stats-grid">
      <el-card class="stat-card">
        <div class="stat-icon blue">
          <UserFilled />
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ stats.totalUsers }}</p>
          <p class="stat-label">用户总数</p>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon green">
          <UserFilled />
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ stats.todayActive }}</p>
          <p class="stat-label">今日活跃</p>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon orange">
          <Bell />
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ stats.unreadMessages }}</p>
          <p class="stat-label">未读消息</p>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon purple">
          <ArrowUp />
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ stats.avgResponseTime }}ms</p>
          <p class="stat-label">平均响应时间</p>
        </div>
      </el-card>
    </div>

    <div class="info-cards">
      <el-card title="系统概览" class="info-card">
        <el-timeline>
          <el-timeline-item timestamp="刚刚" placement="top">
            <el-card size="small">系统启动成功</el-card>
          </el-timeline-item>
          <el-timeline-item timestamp="5分钟前" placement="top">
            <el-card size="small">用户 admin 登录系统</el-card>
          </el-timeline-item>
          <el-timeline-item timestamp="10分钟前" placement="top">
            <el-card size="small">用户数据同步完成</el-card>
          </el-timeline-item>
        </el-timeline>
      </el-card>
      <el-card title="快速操作" class="info-card">
        <div class="quick-actions">
          <el-button type="primary" size="small">新增用户</el-button>
          <el-button size="small">角色管理</el-button>
          <el-button size="small">系统配置</el-button>
          <el-button size="small">操作日志</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { UserFilled, Bell, ArrowUp } from '@element-plus/icons-vue'

const stats = reactive({
  totalUsers: 1256,
  todayActive: 328,
  unreadMessages: 15,
  avgResponseTime: 45
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.stat-icon.blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.green {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.stat-icon.orange {
  background: linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%);
}

.stat-icon.purple {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #666;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin: 4px 0 0 0;
}

.info-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.info-card {
  min-height: 200px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .info-cards {
    grid-template-columns: 1fr;
  }
}
</style>