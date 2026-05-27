<template>
  <div class="table-actions">
    <template v-for="(btn, _idx) in visibleBtns" :key="_idx">
      <el-button v-bind="btn.props" size="small" @click="btn.handler">{{ btn.label }}</el-button>
    </template>
    <el-dropdown v-if="hiddenBtns.length > 0" trigger="hover" placement="bottom-end" @command="handleDropdown">
      <el-button size="small" class="more-btn">
        <span class="vertical-dots">⋮</span>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item v-for="(btn, idx) in hiddenBtns" :key="idx" :command="idx">
            {{ btn.label }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

interface ActionBtn {
  label: string
  props: Record<string, any>
  handler: () => void
  permission?: string | string[]
}

const props = withDefaults(defineProps<{
  max?: number
  buttons: ActionBtn[]
}>(), { max: 3 })

const userStore = useUserStore()

const filteredButtons = computed(() => {
  return props.buttons.filter(btn => {
    if (!btn.permission) return true
    if (Array.isArray(btn.permission)) {
      return btn.permission.some(p => userStore.hasPermission(p))
    }
    return userStore.hasPermission(btn.permission)
  })
})

const visibleBtns = computed(() => {
  return filteredButtons.value.slice(0, props.max)
})

const hiddenBtns = computed(() => {
  return filteredButtons.value.slice(props.max)
})

function handleDropdown(idx: number) {
  filteredButtons.value.slice(props.max)[idx]?.handler()
}
</script>

<style scoped>
.table-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}
.more-btn {
  padding: 5px 8px;
  line-height: 1;
}
.vertical-dots {
  font-size: 18px;
  font-weight: bold;
  letter-spacing: 0;
  line-height: 1;
}
</style>