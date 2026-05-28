# 云枢中台 Java 编码规范

**版本：** v1.0  
**日期：** 2026-05-15  
**参考：** 阿里巴巴 Java 开发手册（嵩山版）+ 云枢中台业务特性  
**适用范围：** 所有后端微服务模块

---

## 一、通用原则

### 1.1 命名规约

| 类型 | 规范 | 正例 | 反例 |
|------|------|------|------|
| **类名** | UpperCamelCase，名词 | `UserService` `OrderController` | `userService` `order_controller` |
| **方法名** | lowerCamelCase，动词 | `getUserById()` `createOrder()` | `GetUserById()` `create_order()` |
| **变量名** | lowerCamelCase，有意义 | `userName` `orderList` | `uname` `list1` `temp` |
| **常量名** | 全大写，下划线分隔 | `MAX_RETRY_COUNT` `DEFAULT_PAGE_SIZE` | `maxRetry` `defaultPageSize` |
| **包名** | 全小写，点分隔 | `com.cloudhub.platform.user` | `com.cloudHub.platform.User` |
| **布尔变量** | 避免 is 前缀（部分框架序列化问题） | `deleted` `enabled` | `isDeleted` `isEnabled` |
| **枚举类名** | Enum 后缀 | `StatusEnum` `RoleTypeEnum` | `Status` `RoleType` |
| **异常类名** | Exception 后缀 | `BizException` `NotFoundException` | `BizError` `NotFound` |
| **抽象类名** | Abstract 前缀或 Base 前缀 | `AbstractService` `BaseEntity` | `ServiceAbstract` |
| **测试类名** | 被测试类名 + Test 后缀 | `UserServiceTest` | `TestUserService` |

### 1.2 代码格式

```java
// ✅ 缩进：4 个空格（禁止 Tab）
public class UserService {
    private static final int MAX_RETRY = 3;
    
    // ✅ 大括号：换行，左大括号不换行
    public User getUserById(Long id) {
        if (id == null || id <= 0) {
            throw new BizException("用户ID不能为空");
        }
        
        // ✅ 运算符前后加空格
        int count = MAX_RETRY + 1;
        
        // ✅ 逗号后加空格
        List<String> roles = Arrays.asList("ADMIN", "USER", "GUEST");
        
        return userMapper.selectById(id);
    }
}
```

**格式检查清单：**
- [ ] 使用 4 个空格缩进（编辑器设置 `Tab Size = 4`, `Indent Using Spaces`）
- [ ] 行宽不超过 120 字符
- [ ] 左大括号不换行，右大括号独占一行
- [ ] if/for/while/switch/do 必须加大括号，即使只有一行
- [ ] 运算符前后加空格
- [ ] 逗号、分号后加空格
- [ ] 点号（.）前后不加空格

### 1.3 OOP 规约

```java
// ✅ 使用包装类型（避免 NPE 风险）
private Long userId;      // 推荐
private long userId;      // 不推荐（默认值为 0，无法区分 null）

// ✅ 使用 BigDecimal 进行金额计算
BigDecimal amount = new BigDecimal("0.1");  // ✅ 字符串构造
BigDecimal result = amount.add(new BigDecimal("0.2"));
// BigDecimal amount = new BigDecimal(0.1); // ❌ 浮点构造，精度丢失

// ✅ 重写 equals 必须重写 hashCode
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return Objects.equals(id, user.id);
}

@Override
public int hashCode() {
    return Objects.hash(id);
}

// ✅ 使用 StringBuilder 进行字符串拼接（循环内）
StringBuilder sb = new StringBuilder();
for (String item : items) {
    sb.append(item).append(",");
}
// 避免：String result = ""; result += item; // 每次循环创建新对象

// ✅ 慎用 Object 的 clone 方法（浅拷贝风险）
// 推荐使用：
// 1. 拷贝构造器
// 2. 序列化反序列化（深拷贝）
// 3. MapStruct / BeanUtils（Spring 的 copyProperties 也是浅拷贝）
```

### 1.4 集合处理

```java
// ✅ 使用泛型，避免 Raw Type
List<String> names = new ArrayList<>();  // ✅
List names = new ArrayList();            // ❌

// ✅ 集合初始化时指定容量
List<User> users = new ArrayList<>(100);  // ✅ 预知大小
Map<String, Object> map = new HashMap<>(16); // ✅ 默认负载因子 0.75

// ✅ 返回空集合而非 null
public List<User> getUsers() {
    List<User> users = userMapper.selectList();
    return users == null ? Collections.emptyList() : users; // ✅
    // return users; // ❌ 可能返回 null，调用方需判空
}

// ✅ 使用 Java 8+ 的集合操作
List<String> activeNames = users.stream()
    .filter(u -> u.getStatus() == 1)
    .map(User::getName)
    .distinct()
    .collect(Collectors.toList());

// ✅ 使用 Map 的 computeIfAbsent
Map<String, List<User>> groupMap = new HashMap<>();
// 避免：
// List<User> list = groupMap.get(key);
// if (list == null) { list = new ArrayList<>(); groupMap.put(key, list); }
// list.add(user);
groupMap.computeIfAbsent(deptId, k -> new ArrayList<>()).add(user); // ✅
```

### 1.5 并发处理

```java
// ✅ 使用 ThreadPoolExecutor 手动创建线程池（避免 Executors 的隐患）
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    4,                      // 核心线程数
    8,                      // 最大线程数
    60L, TimeUnit.SECONDS,  // 空闲线程存活时间
    new LinkedBlockingQueue<>(100), // 任务队列
    new ThreadFactoryBuilder().setNameFormat("order-pool-%d").build(), // 线程工厂
    new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
);

// ✅ 使用 CompletableFuture 进行异步编排
CompletableFuture<User> userFuture = CompletableFuture
    .supplyAsync(() -> userService.getUserById(userId), executor);
CompletableFuture<Order> orderFuture = CompletableFuture
    .supplyAsync(() -> orderService.getOrderById(orderId), executor);

CompletableFuture<Void> combined = CompletableFuture
    .allOf(userFuture, orderFuture)
    .thenRun(() -> {
        User user = userFuture.join();
        Order order = orderFuture.join();
        // 处理业务
    });

// ✅ 使用 volatile + DCL 实现单例（高并发场景）
private volatile static Singleton instance;
public static Singleton getInstance() {
    if (instance == null) {
        synchronized (Singleton.class) {
            if (instance == null) {
                instance = new Singleton();
            }
        }
    }
    return instance;
}
```

### 1.6 异常处理

```java
// ✅ 异常分层
// 业务异常（用户可感知）
public class BizException extends RuntimeException {
    private final String code;
    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }
}

// 系统异常（内部错误）
public class SysException extends RuntimeException {
    public SysException(String message, Throwable cause) {
        super(message, cause);
    }
}

// ✅ Controller 层统一异常处理
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.fail("SYSTEM_ERROR", "系统繁忙，请稍后重试");
    }
}

// ✅ 捕获异常后不要忽略
public void process() {
    try {
        // 业务逻辑
    } catch (IOException e) {
        log.error("处理失败: ", e);  // ✅ 记录日志
        throw new BizException("PROCESS_ERROR", "处理失败: " + e.getMessage()); // ✅ 转换后抛出
        // ❌ 不要：e.printStackTrace();
        // ❌ 不要：catch 块为空
    }
}
```

### 1.7 日志规约

```java
// ✅ 使用 SLF4J + Logback
@Slf4j
public class UserService {
    
    public void createUser(User user) {
        // ✅ 使用占位符，避免字符串拼接
        log.info("创建用户: userId={}, name={}, deptId={}", 
                 user.getId(), user.getName(), user.getDeptId());
        
        // ❌ 避免：
        // log.info("创建用户: userId=" + user.getId() + ", name=" + user.getName());
        // 原因：即使日志级别是 warn，字符串拼接也会执行
        
        try {
            userMapper.insert(user);
        } catch (Exception e) {
            log.error("创建用户失败: userId={}", user.getId(), e); // ✅ 异常作为最后一个参数
        }
    }
    
    // ✅ 生产环境禁止输出 debug（可通过配置控制）
    if (log.isDebugEnabled()) {
        log.debug("详细参数: {}", JsonUtils.toJson(params)); // 大量数据只在 debug 输出
    }
}
```

**日志级别使用规范：**

| 级别 | 使用场景 | 示例 |
|------|---------|------|
| **ERROR** | 影响系统运行的错误 | 数据库连接失败、关键业务异常 |
| **WARN** | 不影响运行但需要注意 | 参数校验失败、重复提交、降级处理 |
| **INFO** | 关键业务节点记录 | 用户登录、订单创建、支付完成 |
| **DEBUG** | 开发调试信息 | 方法入参、SQL 语句、中间状态 |
| **TRACE** | 最详细的跟踪信息 | 循环内部、大量输出的调试 |

---

## 二、分层架构规约

### 2.1 包结构规范

```
com.cloudhub.platform.{module}
├── controller          # 控制器层（API 入口）
│   ├── dto             # 请求/响应 DTO
│   └── vo              # 视图对象 VO
├── service             # 业务逻辑层
│   ├── impl            # 实现类
│   └── dto             # 服务层 DTO
├── domain              # 领域模型层
│   ├── entity          # 实体（对应数据库表）
│   ├── vo              # 值对象
│   └── enums           # 枚举
├── repository          # 数据访问层（Mapper 接口）
│   └── mapper          # MyBatis Mapper
├── infrastructure      # 基础设施层
│   ├── config          # 配置类
│   ├── util            # 工具类
│   └── aspect          # AOP 切面
└── PlatformXxxApplication.java  # 启动类
```

### 2.2 分层职责

| 层级 | 职责 | 禁止事项 |
|------|------|---------|
| **Controller** | 参数校验、调用 Service、返回 VO | 禁止编写业务逻辑 |
| **Service** | 业务编排、事务控制 | 禁止直接操作数据库 |
| **Repository/Mapper** | 数据访问、SQL 映射 | 禁止编写业务逻辑 |
| **Domain/Entity** | 数据载体、简单校验 | 禁止依赖其他层 |
| **Infrastructure** | 工具、配置、切面 | 禁止依赖业务层 |

### 2.3 数据对象转换

```java
// ✅ 使用 MapStruct 进行对象转换（编译时生成代码，性能优秀）
@Mapper(componentModel = "spring")
public interface UserConverter {
    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);
    
    UserVO toVO(User entity);
    
    List<UserVO> toVOList(List<User> entities);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", expression = "java(java.time.LocalDateTime.now())")
    User toEntity(UserCreateDTO dto);
}

// 使用
UserVO vo = UserConverter.INSTANCE.toVO(user);
```

---

## 三、数据库访问规约

### 3.1 MyBatis-Plus 使用规范

```java
// ✅ 实体类规范
@Data
@TableName("t_user")  // 表名前缀 t_
public class User {
    @TableId(type = IdType.ASSIGN_ID)  // 使用雪花算法
    private Long id;
    
    @TableField("user_name")  // 下划线命名映射
    private String userName;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic  // 逻辑删除
    @TableField("is_deleted")
    private Integer deleted;
}

// ✅ Mapper 接口
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 简单 CRUD 使用 BaseMapper 提供的方法
    // 复杂查询自定义 SQL
    
    @Select("SELECT * FROM t_user WHERE dept_id = #{deptId} AND status = #{status}")
    List<User> selectByDeptAndStatus(@Param("deptId") Long deptId, 
                                      @Param("status") Integer status);
    
    // ✅ 分页查询
    IPage<User> selectUserPage(IPage<User> page, @Param("query") UserQuery query);
}

// ✅ Service 层
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    @Override
    public IPage<UserVO> getUserPage(UserQuery query) {
        // 构建查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(query.getName()), User::getName, query.getName())
               .eq(query.getStatus() != null, User::getStatus, query.getStatus())
               .orderByDesc(User::getCreateTime);
        
        // 执行分页查询
        IPage<User> page = new Page<>(query.getPageNum(), query.getPageSize());
        page = baseMapper.selectPage(page, wrapper);
        
        // 转换并返回
        return page.convert(UserConverter.INSTANCE::toVO);
    }
}
```

### 3.2 SQL 编写规范

```xml
<!-- ✅ XML 映射文件 -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.cloudhub.platform.user.repository.mapper.UserMapper">
    
    <!-- ✅ 使用 resultMap 明确映射关系 -->
    <resultMap id="BaseResultMap" type="com.cloudhub.platform.user.domain.entity.User">
        <id column="id" property="id"/>
        <result column="user_name" property="userName"/>
        <result column="create_time" property="createTime"/>
    </resultMap>
    
    <!-- ✅ 使用 sql 片段复用 -->
    <sql id="Base_Column_List">
        id, user_name, email, phone, status, create_time, update_time
    </sql>
    
    <!-- ✅ 分页查询 -->
    <select id="selectUserPage" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM t_user
        WHERE is_deleted = 0
        <if test="query.name != null and query.name != ''">
            AND user_name LIKE CONCAT('%', #{query.name}, '%')
        </if>
        <if test="query.status != null">
            AND status = #{query.status}
        </if>
        ORDER BY create_time DESC
    </select>
    
</mapper>
```

**SQL 编写禁忌：**
- ❌ 禁止使用 `SELECT *`
- ❌ 禁止在 WHERE 子句中对字段进行函数操作（导致索引失效）
- ❌ 禁止隐式类型转换（如字符串字段传数字）
- ❌ 禁止不使用参数的 SQL 拼接（SQL 注入风险）
- ❌ 禁止多表关联超过 3 个（考虑业务拆分或数据冗余）
- ❌ 禁止在循环中执行 SQL（使用批量操作）

---

## 四、安全规约

```java
// ✅ SQL 注入防护：使用 #{} 预编译
@Select("SELECT * FROM t_user WHERE name = #{name}")  // ✅
// @Select("SELECT * FROM t_user WHERE name = '${name}'") // ❌ 危险

// ✅ XSS 防护：输出转义
String safeOutput = HtmlUtils.htmlEscape(userInput);  // Spring 提供

// ✅ 敏感数据脱敏
public class UserVO {
    @JsonSerialize(using = PhoneDesensitizeSerializer.class)
    private String phone;  // 输出：138****8888
    
    @JsonSerialize(using = IdCardDesensitizeSerializer.class)
    private String idCard; // 输出：110101********1234
}

// ✅ 接口防重放（幂等性）
@PostMapping("/orders")
@Idempotent(key = "#request.idempotentKey", expire = 60)  // 自定义注解
public Result<OrderVO> createOrder(@RequestBody OrderCreateRequest request) {
    // 60 秒内相同 key 的请求会被拒绝
}

// ✅ 参数校验
@Data
public class UserCreateRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    private String name;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Min(value = 1, message = "年龄必须大于0")
    @Max(value = 150, message = "年龄不能超过150")
    private Integer age;
}

// Controller 中使用
@PostMapping("/users")
public Result<UserVO> createUser(@RequestBody @Valid UserCreateRequest request) {
    // @Valid 会自动触发校验，失败时抛出 MethodArgumentNotValidException
}
```

---

## 五、性能规约

```java
// ✅ 批量操作（MyBatis-Plus）
// 插入
userService.saveBatch(userList, 500);  // 每 500 条批量插入一次

// 更新
userService.updateBatchById(userList, 500);

// ✅ 缓存使用
@Service
public class UserServiceImpl implements UserService {
    
    @Cacheable(value = "user", key = "#id")  // 查询缓存
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
    
    @CacheEvict(value = "user", key = "#user.id")  // 更新时清除缓存
    public void updateUser(User user) {
        userMapper.updateById(user);
    }
    
    @CacheEvict(value = "user", allEntries = true)  // 批量操作时清除全部
    public void batchUpdate(List<User> users) {
        userService.updateBatchById(users);
    }
}

// ✅ 大字段延迟加载
@TableField(select = false)  // 默认查询不加载
private String largeContent;

// ✅ 分页参数限制
public IPage<User> getPage(PageParam param) {
    // 限制最大分页大小，防止内存溢出
    long pageSize = Math.min(param.getPageSize(), 1000);
    IPage<User> page = new Page<>(param.getPageNum(), pageSize);
    return userMapper.selectPage(page, wrapper);
}
```

---

## 六、单元测试规约

```java
// ✅ 测试类命名
@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    @DisplayName("根据ID查询用户-成功")
    public void getUserById_Success() throws Exception {
        // Given
        Long userId = 1L;
        UserVO mockUser = new UserVO();
        mockUser.setId(userId);
        mockUser.setName("张三");
        when(userService.getUserById(userId)).thenReturn(mockUser);
        
        // When & Then
        mockMvc.perform(get("/api/v1/users/{id}", userId)
                .header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("200"))
            .andExpect(jsonPath("$.data.name").value("张三"));
    }
    
    @Test
    @DisplayName("根据ID查询用户-不存在")
    public void getUserById_NotFound() throws Exception {
        when(userService.getUserById(any())).thenThrow(new NotFoundException("用户不存在"));
        
        mockMvc.perform(get("/api/v1/users/{id}", 999))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }
}
```

---

## 七、代码审查清单

### 7.1 提交前自查

- [ ] 命名符合规范（类/方法/变量/常量）
- [ ] 代码格式正确（缩进/空格/换行）
- [ ] 无死代码（未使用的变量/方法/导入）
- [ ] 无魔法数字（使用常量替代）
- [ ] 空指针安全（Optional/判空/默认值）
- [ ] 资源正确释放（try-with-resources）
- [ ] 线程安全问题（并发容器/同步机制）
- [ ] 敏感信息未硬编码（密码/密钥配置化）
- [ ] 日志使用正确（占位符/级别）
- [ ] 单元测试覆盖核心业务逻辑

### 7.2 审查要点

| 检查项 | 说明 |
|--------|------|
| **代码风格** | 是否符合本规范 |
| **设计合理性** | 是否过度设计/设计不足 |
| **性能影响** | 是否存在 N+1 查询/大对象/死循环 |
| **安全漏洞** | SQL 注入/XSS/越权/敏感信息泄露 |
| **可维护性** | 是否易于理解/修改/测试 |
| **兼容性** | 是否影响现有功能 |

---

## 八、附录

### 8.1 推荐工具

| 工具 | 用途 | 配置 |
|------|------|------|
| **Alibaba Java Coding Guidelines** | IDEA 插件，实时检查规范 | 安装后自动启用 |
| **SonarLint** | 代码质量检查 | 绑定 SonarQube |
| **SpotBugs** | 静态代码分析 | Maven 插件 |
| **Checkstyle** | 代码格式检查 | 统一配置文件 |
| **JaCoCo** | 测试覆盖率 | Maven 插件 |

### 8.2 相关文档

- [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Effective Java (Joshua Bloch)](https://www.oreilly.com/library/view/effective-java-3rd/9780134686097/)

---

**文档版本记录：**

| 版本 | 日期 | 变更内容 | 编制人 |
|------|------|---------|--------|
| v1.0 | 2026-05-15 | 初始版本 | AI 助手 |
