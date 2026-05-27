# 项目进度报告 - 云枢中台工作流

## 项目基本信息
- **项目名称**: 云枢中台 - 工作流引擎
- **模块**: platform-admin (前端) + platform-workflow (后端)
- **当前版本**: v2.0
- **最后更新**: 2026-05-27

## 已完成工作

### 1. 基础架构
- ✅ Flowable 6.8.1 后端集成 (platform-workflow 模块)
- ✅ BPMN 设计器 (自定义 SVG 渲染，非 bpmn-js)
- ✅ Docker Compose 编排 (mysql, redis, nacos, kafka, es, 前端, 后端)
- ✅ Nginx 反向代理

### 2. 核心功能
- ✅ 流程定义 CRUD (部署/挂起/激活/删除/编辑)
- ✅ 流程设计器 (拖拽、连线、缩放、撤销/重做)
- ✅ 属性面板 (常规/候选配置/送审配置/审批规则)
- ✅ 流程导出 BPMN XML
- ✅ 流程实例发起/查询/删除
- ✅ 待办/已办任务查询
- ✅ 任务审批通过/驳回/转办
- ✅ 审批进度时间轴

### 3. 候选人配置 (2026-05-25~27)
- ✅ 候选范围过滤（公司/本部门/集团）
- ✅ 组织树弹窗选择器 (el-tree + checkbox)
- ✅ 人员/岗位互斥选择
- ✅ 搜索框 + 扁平结果 + 分页
- ✅ 弹窗已选栏 + 标签式展示
- ✅ XML 解析候选人回显（DOM childNodes + localName 遍历）
- ✅ 候选人选择器弹窗树复选框同步 (setCheckedKeys)

### 4. 请假申请模块 (2026-05-27)
- ✅ 请假流程 BPMN 统一格式 (`bpmn:` 前缀 + `candidateUsers`)
- ✅ 请假申请列表（分页表格）
- ✅ 新增申请弹窗
- ✅ 查看流程（完整节点链 + 当前节点标记）
- ✅ 申请详情（请假信息 + 审批时间轴）
- ✅ 时间格式统一 yyyy-MM-dd HH:mm:ss

### 5. 部署与运维
- ✅ Docker 容器化部署 (nginx + eclipse-temurin:21)
- ✅ Kafka 消息队列集成 (任务创建/完成通知)
- ✅ Kafka Docker 连接配置 (容器名访问 + 超时控制)

## 待完成工作

### 高优先级
- 🔲 **候选范围Tab切换** - 弹窗内 Tab 切换人员/岗位模式，无需关闭重开
- 🔲 **Kafka 消费端** - 前端接收待办通知展示

### 中优先级
1. **画布交互优化**
   - 节点拖拽移动流畅性
   - 连线自动布局
   - 网格对齐

2. **流程验证与发布**
   - BPMN 语法校验
   - 版本管理

3. **用户任务签收**
   - candidateUsers 支持多人抢签
   - claim/unclaim API 前端集成

### 低优先级
4. **高级功能**
   - 子流程支持
   - 条件表达式编辑器
   - 流程模拟/预览
   - 协作编辑

## 技术栈
- **前端**: Vue 3 + TypeScript + Element Plus + Pinia
- **后端**: Spring Boot 3.2 + Flowable 6.8.1 + MyBatis-Plus
- **数据库**: MySQL 8.0
- **中间件**: Redis, Nacos, Kafka, Elasticsearch
- **部署**: Docker Compose + Nginx

## 关键文件
- `code/platform-admin/src/components/ProcessDesigner.vue` - 流程设计器
- `code/platform-admin/src/views/workflow/Leave.vue` - 请假申请
- `code/platform-admin/src/views/workflow/Definition.vue` - 流程定义管理
- `code/platform-admin/src/views/workflow/TaskTodo.vue` - 我的待办
- `code/platform-admin/src/views/workflow/Monitor.vue` - 流程监控
- `code/platform-admin/src/api/workflow.ts` - 工作流 API
- `code/platform-server/platform-workflow/` - 后端工作流服务

## 访问地址
- 前端: http://localhost:8080
- 后端 API: http://localhost:8081 (user), http://localhost:8084 (workflow)
