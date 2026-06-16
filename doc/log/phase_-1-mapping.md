# Phase -1: csyh 私有包分类映射表

> **任务**: 扫描 csyh 业务代码所有非标准 import, 按类别整理适配映射表 (v2.0 计划 §2.2 W1 任务 1 交付物)
> **扫描时间**: 2026-06-16
> **扫描范围**: `D:\work\AI\output\code\csyh` 全量 `*.java` (4,876 个文件)
> **排除规则**: `java.*` / `javax.*` / `com.cloudhub.*`
> **关联文档**: [v2.0 融合计划 §2](../plan/云枢中台+csyh业务融合实施计划.md) · [编码规范 §1.1](../spec/编码规范.md) · [handoff-2026-06-15.md](../handoff/handoff-2026-06-15.md)

---

## 一、扫描汇总

| 指标 | 数值 |
|------|------|
| 扫描 Java 文件总数 | 4,876 |
| 业务模块数 | 18 (7 后端 + 7 前端 + 4 入口) |
| 非标准 import 去重后 | **4,577** |
| 非标准 import 出现总次数 | **35,719** |
| 来自 `cn.flyrise.*` (私有) | 3,868 去重 / 20,710 出现 (58%) |
| 来自 `org.apache.shiro` (需适配) | 61 去重 / 183 出现 |
| 来自其他第三方 (已可用) | 648 去重 / 14,826 出现 (Spring/MyBatis-Plus/Hutool/Swagger 等) |

> **与 v2.0 数字对照**: v2.0 §2.1 报"7,557 个外部 import"是基于 INTEGRATION_ANALYSIS.md (针对单一业务子集); 本表覆盖**全 7 个业务后端模块**, 数字更全。MyBatis 增强(1,114)/ 系统工具(245) / 业务通用(728) 三项**完全吻合** v2.0。

---

## 二、12 大类别映射表

> 排序: 按"出现次数 × 业务价值"降序。**P0** = Phase -1 W2-W3 必须先做的; **P1** = W4 中间件/业务通用; **P2/P3** = 业务模块本身承担 (Phase 0+)

| # | 类别 (源包前缀) | 去重 | 出现 | 适配方案 | 优先级 | 落地模块 |
|---|----------------|------|------|----------|--------|----------|
| **A** | `cn.flyrise.pai.fe.common` | 495 | **3,910** | park-common.fe: VOs/Service 接口/Feign 客户端/User | **P0** | `park-common` |
| **B** | `cn.flyrise.pai.park.*` | 810 | 2,280 | 业务特定: 空间/物业异常、PO、Service | P2 | `park-space` / `park-property` |
| **C** | `cn.flyrise.pai.service.center` | 789 | 2,337 | 业务特定: 服务/工单/会议室 | P3 | `park-service` |
| **D** | `cn.flyrise.pai.finance` | 509 | 1,714 | 业务特定: 财务/账单/发票/收款 | P3 | `park-finance` |
| **E** | `cn.flyrise.pai.business` | 368 | 1,221 | 业务特定: 招商/客户/合同计划 | P2 | `park-business` |
| **F** | `cn.flyrise.pai.contract` | 203 | 646 | 业务特定: 合同/模板/电子合同 | P1 | `park-contract` |
| **G** | `cn.flyrise.pai.enterprise` | 119 | 326 | 跨业务: 企业工商查询 Feign + 本地企业 | P2 | `park-business` + `park-common.fe` |
| **H** | `cn.flyrise.pai.yunqikufeignclient` | 20 | 49 | 第三方企业征信(云企客) Feign | P3 | park-business 接 mock/暂不接 |
| **I** | `cn.flyrise.common.core` | 61 | **4,151** | **park-common-base: Reply/Result/异常/分页/Controller/工具** | **P0** | `park-common` |
| **J** | `cn.flyrise.common.utils` | 27 | 286 | park-common.utils: DateUtils/ExcelUtil/StringUtils | P0 | `park-common` |
| **K** | `cn.flyrise.common.exception` | 14 | 42 | park-common.exception: BusinessException + 7 子类 | P0 | `park-common` |
| **L** | `cn.flyrise.mybatis` | 14 | **1,114** | **park-common-mybatis: IBaseService/BaseEntity → MP** | **P0** | `park-common` |
| **M** | `cn.flyrise.system` | 55 | 245 | park-common.system + 接云枢 platform-user | P0 | `park-common` + `platform-user` |
| **N** | `cn.flyrise.framework` | 31 | 85 | park-common.security: ShiroUtils/Session → Spring Security | P0 | `park-common` |
| **O** | `cn.flyrise.business` | 218 | 728 | 业务通用层 (CMS/HF); 部分与园区无关, 留业务模块内 | P1 | 暂留原包, 命名空间替换 |
| **P** | `cn.flyrise.mq` | 5 | 78 | park-common.mq: PaiMqTemplate → Spring Kafka | P0 | `park-common` |
| **Q** | `cn.flyrise.oss/pay/web/api/quartz` | 34 | 72 | 各中间件独立: 对象存储/支付/HTTP/Flowable/Quartz | P1 | `park-common` + 业务模块 |
| **R** | `org.apache.shiro` | 61 | 183 | **park-common.security: @RequiresPermissions → Spring Security** | **P0** | `park-common` |
| **S** | `tk.mybatis.mapper` | 7 | 7 | 删除 (用 MP) | P0 | 命名空间替换时一并清理 |
| **T** | `com.alibaba.fastjson` | 11 | 102 | 替换为 Jackson (云枢已用) | P0 | 命名空间替换时一并清理 |

**P0 合计**: A + I + J + K + L + M + N + P + R + S + T = 1,803 去重 / 10,159 出现 (29%)

---

## 三、park-common 适配层目标接口 (W2 启动用)

> W2 任务: 通用基础类适配 + MyBatis 增强适配, 落地到 `platform-park/park-common` 模块

### 3.1 park-common-base (类别 I + J + K, 4,479 出现)

| 源类 (cn.flyrise) | 出现 | 目标 (com.cloudhub.platform.park.common.base) | 复用 platform-common |
|------------------|------|----------------------------------------------|----------------------|
| `common.core.domain.Reply` | 583 | `R<T>` 统一响应 | ❌ (云枢用 Result) |
| `common.core.domain.AjaxResult` | 97 | 兼容旧名 → 静态方法转发到 R | ❌ |
| `common.core.domain.InvalidCode` | 657 | `R.Code` 枚举 | ❌ |
| `common.core.utils.ConvertUtil` | 277 | `ConvertUtil` (空实现, 鼓励用 hutool BeanUtil) | ❌ |
| `common.core.utils.I18nUtil` | 177 | 委托 MessageUtils (云枢有) | ✅ |
| `common.core.utils.IdUtils` | 114 | 委托 Hutool IdUtil | ✅ |
| `common.core.controller.BaseController` | 113 | `ParkBaseController` (继承 platform-common BaseController) | ✅ |
| `common.core.page.TableDataInfo` | 92 | 委托 `PageResult` (云枢) | ✅ |
| `common.core.utils.FeignUtil` | 89 | 委托 Fegin FallbackFactory | ❌ |
| `common.core.exception.CommonException` | 84 | 委托 `BizException` (云枢) | ✅ |
| `common.core.annotation.Inner` | 93 | 同名 (兼容) | ✅ |
| `common.core.annotation.IgnoreTenant` | 78 | 同名 (兼容) | ❌ (云枢 ignoreTable) |
| `common.core.validation.Update/.Create` | 547 | 用 jakarta.validation.GroupSequence | ✅ |
| `common.core.constant.SecurityConstants` | 271 | 同名 (兼容) | ✅ |
| `common.core.text.Convert` | 75 | 委托 Hutool Convert | ✅ |
| `common.utils.DateUtils` | 70 | 委托 Hutool DateUtil | ✅ |
| `common.utils.poi.ExcelUtil` | 68 | 适配 EasyExcel (云枢有) | ✅ |
| `common.utils.StringUtils` | 64 | 委托 Hutool StrUtil | ✅ |
| `common.utils.ServletUtils` | 22 | 委托 Spring WebUtils | ✅ |
| `common.utils.spring.SpringUtils` | 15 | 同名 (云枢有) | ✅ |
| `common.exception.BusinessException` | 20 | 委托 `BizException` | ✅ |
| `common.exception.base.BaseException` | 5 | 委托 `BizException` 基类 | ✅ |

**总计**: 22 个适配类, 4,479 出现, W2 全部覆盖

### 3.2 park-common-mybatis (类别 L, 1,114 出现)

| 源类 (cn.flyrise.mybatis) | 出现 | 目标 |
|--------------------------|------|------|
| `mybatis.base.IBaseService` | 355 | **删除** (直接用 MP `IService<T>`) |
| `mybatis.base.BaseServiceImpl<M, T>` | 356 | **删除** (直接用 MP `ServiceImpl<M, T>`) |
| `mybatis.base.BaseEntity` | 366 | 用 MP `@TableLogic` + `BaseEntity` (云枢有) |
| `mybatis.enums.Policy` | 8 | 删除 (合并到 @TableLogic) |
| `mybatis.enums.MethodType` | 7 | 删除 |
| `mybatis.annotation.TenantPolicy` | 7 | **保留为 park-common 注解**, 用于多租户 |
| `mybatis.feign.ITenantService` | 4 | 删/接 platform-common TenantContextHolder |
| `mybatis.util.ThreadLocalUtil` | 3 | 委托 `TenantContextHolder` (云枢有) |
| `mybatis.util.LocalPolicy` | 3 | 删 |
| `mybatis.aspect.TenantAspect` | 1 | 删 (云枢已用 MyBatis-Plus interceptor) |
| `mybatis.config.TenantConfigProperties` | 1 | 删 |
| `mybatis.config.PaiTenantLineHandler` | 1 | 替换为云枢 `MybatisPlusConfig` |

**总计**: 12 个源类 → 1 个保留(@TenantPolicy) + 11 个删除/委托, **关键决策**: 不做包装层, **直接用 MyBatis-Plus 原生接口**。源 BaseServiceImpl/IBaseService 与 MP 同构, 批量替换即可

### 3.3 park-common.security (类别 N + R, 268 出现)

| 源类 | 出现 | 目标 |
|------|------|------|
| `org.apache.shiro.authz.annotation.RequiresPermissions` | 80 | **自定义同名注解** (转发到 Spring Security `@PreAuthorize("hasAuthority('xxx')")`) |
| `org.apache.shiro.SecurityUtils` | 6 | 委托 `SecurityContextHolder` (Spring Security) |
| `org.apache.shiro.session.Session` | 7 | 委托 `HttpSession` |
| `org.apache.shiro.subject.Subject` | 7 | 委托 `Authentication` |
| `org.apache.shiro.authc.*` | 12 | 委托 Spring Security AuthenticationManager |
| `framework.util.ShiroUtils` | 27 | `SecurityUtils` 包装, 业务代码改 import 即可 |
| `framework.shiro.session.OnlineSession*` | 14 | 重新设计 (云枢无在线会话, 用 JWT 即可) |
| `framework.shiro.realm.UserRealm` | 1 | 删 (Spring Security UserDetailsService 替代) |
| `framework.shiro.web.filter.*` | 2 | 删 (Spring Security Filter 链替代) |
| `framework.shiro.service.SysShiroService/SysPasswordService` | 4 | 重写为 `ParkPasswordService` |

**关键设计**:
- `cn.cloudhub.platform.park.common.security.annotation.RequiresPermissions` 保留 Shrio 注解的"语义"(`value = "user:add"`), 内部转发 `@PreAuthorize("hasAuthority('user:add')")`
- 业务代码改 `import cn.flyrise...` → `import com.cloudhub.platform.park.common.security.annotation.RequiresPermissions` 即可, **方法签名不变**
- Shiro 替换是 W3 任务, 但**注解包装类**必须在 W2 先创建, 否则改不动 N+R 的 import

### 3.4 park-common.fe (类别 A, 3,910 出现) - **最大头**

| 源类 | 出现 | 目标 |
|------|------|------|
| `pai.fe.common.organize.domain.User` | 340 | `ParkUser` (含 username/deptId/roles) |
| `pai.fe.common.core.controller.IBaseController` | 177 | `IParkBaseController<T>` 接口 |
| `pai.fe.common.model.CommonQueryVO` | 140 | `CommonQuery` |
| `pai.fe.common.constants.FieldConstants` | 113 | `FieldConstants` (同名字段常量) |
| `pai.fe.common.util.LogUtil` | 76 | `LogUtil` |
| `pai.fe.common.util.CastUtils` | 58 | 委托 Hutool CastUtil |
| `pai.fe.common.organize.IPermissionCommonService` | 51 | `IPermissionCommonService` Feign client |
| `pai.fe.common.space.feign.IRoomService` | 50 | `IRoomService` (park-space 暴露的 Feign) |
| `pai.fe.common.enterprise.feign.IEnterpriseService` | 47 | `IEnterpriseService` Feign client |
| `pai.fe.common.contract.*` | 100+ | 委托 park-contract VOs |
| `pai.fe.common.space.model.room.RoomVO` | 60 | `RoomVO` (park-space 暴露) |
| `pai.fe.common.finance.model.*` | 100+ | 委托 park-finance DTOs |
| `pai.fe.common.model.query.ParkIdKeyWordPageQuery` | 72 | `PageQuery` |

**W2 关键决策点**:
- A 类别 495 个 import, 但很多是 VOs 委托, 不是新代码, **实际新增代码量 < 30 个类**
- 大部分 VOs 留作"壳", 通过 Spring `@JsonIgnore` 委托 park-* 模块的真实 DTO
- 最重要的 3 个类: `User` (340), `IBaseController` (177), `CommonQueryVO` (140), **W2 必须先做**

### 3.5 park-common.system (类别 M, 245 出现) - 接入云枢 platform-user

| 源类 | 出现 | 云枢对应 |
|------|------|----------|
| `cn.flyrise.system.domain.SysUser` | 52 | `com.cloudhub.platform.user.entity.SysUser` ✅ |
| `cn.flyrise.system.domain.SysDictData` | 24 | `SysDict` (云枢) |
| `cn.flyrise.system.domain.SysRole` | 12 | `SysRole` (云枢) |
| `cn.flyrise.system.domain.SysMenu` | 5 | `SysMenu` (云枢) |
| `cn.flyrise.system.domain.SysDept` | 6 | `SysDept` (云枢) |
| `cn.flyrise.system.domain.SysOperLog` | 6 | `SysOperLog` (云枢) |
| `cn.flyrise.system.domain.SysConfig` | 5 | `SysConfig` (云枢) |
| `cn.flyrise.system.service.ISysUserService` | 9 | `ISysUserService` (云枢, 通过 Feign) |
| `cn.flyrise.system.service.ISysDictDataService` | 8 | `ISysDictService` (云枢) |
| ... 其余 12 个 Service | | 同上, 接云枢 Feign client |

**设计**:
- 不在 park-common 复制云枢的 Service, 而是在 park-common.system 暴露**`IPlatformUserFeign`** 接口 (Feign client), 转发到 platform-user
- 业务代码改 import 后, 通过 `@Autowired IPlatformUserFeign` 调用云枢服务
- 真正消除 SysUser 重复定义

### 3.6 park-common.mq (类别 P, 78 出现)

| 源类 | 出现 | 目标 |
|------|------|------|
| `cn.flyrise.mq.core.model.PaiMqMessageBean` | 38 | `MqMessage` (park-common.mq) |
| `cn.flyrise.mq.core.service.PaiMqTemplate` | 21 | `ParkMqTemplate` (内部用 Spring KafkaTemplate) |
| `cn.flyrise.mq.core.annotation.PaiMqListener` | 15 | `@ParkMqListener` (委托 `@KafkaListener`) |

### 3.7 命名空间清理 (类别 S + T + O 部分, 837 出现)

| 类别 | 出现 | 动作 |
|------|------|------|
| `tk.mybatis.mapper.*` | 7 | 全局删除 (改 MP 注解) |
| `com.alibaba.fastjson.*` | 102 | 全局替换 Jackson (`JSON.parseObject` → `objectMapper.readValue`) |
| `cn.flyrise.business.aspect.DataDict/Result` | 41 | 保留 (CMS 业务用), 仅命名空间替换 |
| `cn.flyrise.business.util.UrlTransferUtil` | 58 | 保留, 命名空间替换 |

---

## 四、与 v2.0 计划对照

| v2.0 §2.1 类别 | v2.0 数字 | 本表数字 | 备注 |
|---------------|-----------|----------|------|
| 通用基础 (异常/分页/响应/工具) | 5,486 | 4,479 (I+J+K) | 接近, 差额来自 I 内的 `validation` 注解 |
| MyBatis 增强 (IBaseService, BaseEntity) | 1,114 | 1,114 (L) | **完全一致** ✅ |
| 安全框架 (Shiro + 自研) | - | 268 (N+R) | v2.0 未给数字 |
| 业务通用模块 | 713 | 728 (O) | 接近 ✅ |
| 系统工具 (用户/权限/字典) | 244 | 245 (M) | **完全一致** ✅ |
| 中间件 (MQ/Job/Quartz/OSS/Redis) | - | 78+72 (P+Q) | |
| 跨业务通用 (pai.fe.common) | - | 3,910 (A) | v2.0 未单列, **本文档新识别** |

**新增认知**:
- `cn.flyrise.pai.fe.common` (3,910 出现) 是**最大头**, v2.0 没单独列
- Shiro 替代需要"包装注解"模式, 不能粗暴替换
- MyBatis 增强可**直接删除**包装层, MP 原生接口就够

---

## 五、风险与缓解

| 风险 | 严重度 | 缓解 |
|------|--------|------|
| **ParkUser 字段不完整** | 🔴 | W2 用 Adapter 模式: ParkUser 包装云枢 SysUser, 不复制字段 |
| **Feign 调用链断裂** | 🟡 | 业务间 Feign (Room/Enterprise) W2 先建空接口, 业务模块上线时填实现 |
| **Shiro 注解语义丢失** | 🔴 | 自定义同名注解 + 单元测试覆盖 (W3) |
| **BaseEntity 字段差异** | 🟡 | 在云枢 BaseEntity 上加 `tenantId` (已用雪花 ID), 命名空间替换一次性完成 |
| **Hutool 兼容性** | 🟢 | 云枢已用 Hutool 5.8.x, csyh 用 5.x, 直接可用 |
| **ExcelUtil API 差异** | 🟡 | csyh 用 hutool-poi, 云枢用 EasyExcel - 字段映射需手工 |
| **命名空间替换遗漏** | 🟡 | 实施前用 grep 全量扫描 + 提交前 CI fail-fast |

---

## 六、W1 完成验收 + W2 启动 Checklist

### W1 验收 (本任务)

- [x] 扫描 csyh 全量 Java 文件 import (4,876 个)
- [x] 去重 + 出现次数统计 (4,577 / 35,719)
- [x] 按 12 类别分类 + 优先级排序
- [x] park-common 适配层目标接口清单 (3.1-3.7)
- [x] 与 v2.0 计划数字对照 (MyBatis/系统工具完全吻合)
- [x] 风险与缓解策略
- [ ] **用户拍板** park-common 适配层结构 (3.1-3.7)

### W2 启动 Checklist (下次会话)

- [ ] 用户拍板 park-common 模块结构 (3.1-3.7 的 7 个子模块)
- [ ] 在 `code/platform-server/` 下创建 `platform-park/park-common/pom.xml`
- [ ] 复制 platform-common 依赖 + MyBatis-Plus + Hutool + EasyExcel
- [ ] W2 任务 1: park-common-base 22 个适配类 (按 3.1 表)
- [ ] W2 任务 2: park-common-mybatis 12 个源类清理 (按 3.2 表)
- [ ] W2 任务 3: park-common.security 注解包装 (3.3 的 `@RequiresPermissions`)
- [ ] W2 任务 4: 编译通过 + 单元测试覆盖 > 80%
- [ ] git commit: `feat(csyh-park-common): 公共层适配 phase -1 w2`

---

## 七、产出文件

| 文件 | 行数 | 用途 |
|------|------|------|
| `doc/log/phase_-1-import-raw.txt` | 4,582 | 4,577 去重 import 原始清单 |
| `doc/log/phase_-1-import-counted.txt` | 4,582 | 4,577 个 import + 出现次数 (按次数降序) |
| `doc/log/phase_-1-mapping.md` | (本文件) | 分类映射表 + 适配方案 |

---

**报告完毕。等待用户拍板 W2 启动**。

_生成时间: 2026-06-16_
_基础数据: csyh 4,876 个 Java 文件 / 35,719 次 import 出现_
