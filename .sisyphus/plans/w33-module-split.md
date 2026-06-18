# W3.3: 独立 Maven 模块拆分

## Goal
将 Room/Building/Contract 从 platform-user 拆分成 3 个独立 Maven 模块，各自独立进程部署。

## 架构

```
code/platform-server/
├── park-space/          (port 8091) - Room 实体 + CRUD + 状态机
├── park-property/       (port 8092) - Building 实体 + CRUD
├── park-contract/       (port 8093) - Contract 实体 + CRUD + 状态机
└── ...现有模块不变
```

## Phase 1: 创建 3 个 Maven 模块骨架
- [ ] 创建目录结构 (park-space / park-property / park-contract)
- [ ] 每个模块: pom.xml, Application.java, application.yml
- [ ] 更新父 pom.xml modules 列表

## Phase 2: 迁移代码 + 改包名
- [ ] park-space: Room entity/mapper/service/controller + V28 migration + tests
- [ ] park-property: Building entity/mapper/service/controller + V29 migration + tests
- [ ] park-contract: Contract entity/mapper/service/controller + V30 migration + tests

## Phase 3: 从 platform-user 中删除旧代码
- [ ] 删除 Room/Building/Contract 相关 18 个文件 (entity/mapper/xml/service/controller/test + V28-V30 migrations)

## Phase 4: 基础设施
- [ ] Dockerfile.space / Dockerfile.property / Dockerfile.contract
- [ ] CI: 加 JAR 上传 + 镜像构建+推送
- [ ] docker-compose.yml: 加 3 个新服务

## Phase 5: 网关路由更新
- [ ] /room/** → park-space (8091)
- [ ] /building/** → park-property (8092)
- [ ] /contract/** → park-contract (8093)

## Phase 6: 编译验证
- [ ] mvn compile -o 无错误
- [ ] 各模块测试通过
- [ ] LSP diagnostics clean

## Phase 7: 提交 + 推送
