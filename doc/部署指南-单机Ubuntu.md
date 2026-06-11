# 云枢中台 · 单机 Ubuntu 部署指南

> **服务器**: 8C16G, Ubuntu, Docker 已安装
> **部署方式**: Docker Compose 单机（3 文件叠加：基础 + ghcr 镜像 + 生产配置）
> **配套**: `docker-compose.yml` + `docker-compose.ghcr.yml` + `docker-compose.prod.yml`

---

## 1. 服务器前置准备

### 1.1 检查 Docker 版本

```bash
docker --version        # 需 ≥ 24.0
docker compose version  # 需 ≥ 2.20
```

### 1.2 配置 Docker 镜像加速（可选，ghcr.io 在国内可能慢）

```bash
# 编辑 /etc/docker/daemon.json
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": ["https://mirror.ccs.tencentyun.com"],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
EOF
sudo systemctl restart docker
```

### 1.3 开放防火墙端口

```bash
# 管理后台
sudo ufw allow 8080/tcp  # platform-admin
sudo ufw allow 8090/tcp  # platform-ops-admin

# API 网关（前端调后端）
sudo ufw allow 8083/tcp  # platform-gateway

# 中间件管理（内网访问时可不开放）
sudo ufw allow 8848/tcp  # Nacos 控制台
sudo ufw allow 5601/tcp  # Kibana
sudo ufw allow 9001/tcp  # MinIO 控制台
sudo ufw allow 8089/tcp  # Kafka-UI
sudo ufw allow 9411/tcp  # Zipkin (M6 P1-1)
```

### 1.4 配置系统参数（ES 和 Kafka 需要）

```bash
# 虚拟内存映射（Elasticsearch 需要）
sudo sysctl -w vm.max_map_count=262144
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf

# 文件句柄限制
sudo tee -a /etc/security/limits.conf <<-'EOF'
* soft nofile 65536
* hard nofile 65536
EOF
```

---

## 2. 获取项目文件

```bash
# 方式一：git clone（推荐，便于后续更新）
git clone https://github.com/fkeu520/cloudplatform.git /opt/platform
cd /opt/platform

# 方式二：如果内网不通，从开发机 scp 整个目录过去
# 在开发机执行：
# cd D:\work\AI\output\platform
# tar czf platform.tar.gz --exclude='.git' --exclude='node_modules' --exclude='target' .
# scp platform.tar.gz ubuntu@<server-ip>:/opt/
# 在服务器执行：
# cd /opt && tar xzf platform.tar.gz
```

---

## 3. ghcr.io 认证（拉取预构建镜像）

```bash
# 创建 Personal Access Token（在 GitHub: Settings → Developer settings →  Personal access tokens → Fine-grained tokens）
# 权限：Contents: read

# 登录 ghcr.io
echo <your-github-token> | sudo docker login ghcr.io -u <your-github-username> --password-stdin
```

---

## 4. 创建 .env 生产环境变量文件

```bash
# 在 /opt/platform 目录下创建
cat > .env <<'EOF'
# MySQL
MYSQL_ROOT_PASSWORD=cloudhub_root_2026
MYSQL_PASSWORD=platform123

# Nacos 认证
NACOS_AUTH_TOKEN=$(openssl rand -base64 32)
NACOS_AUTH_IDENTITY_VALUE=cloudhub-secret-key

# MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin123

# JWT（重要：生产必须改！）
JWT_SECRET=cloudhub-platform-secret-key-2024
EOF

chmod 600 .env  # 权限保护
```

---

## 5. 调整为 8C16G 资源配置

`docker-compose.prod.yml` 默认是 4C8G 配置，需要升级到 8C16G：

```bash
cd /opt/platform

# 备份原文件
cp docker-compose.prod.yml docker-compose.prod.yml.bak

# 执行 8C16G 升级脚本
sed -i 's/memory: 512M/memory: 1G/g' docker-compose.prod.yml
sed -i 's/memory: 768M/memory: 1.5G/g' docker-compose.prod.yml
sed -i 's/memory: 1536M/memory: 3G/g' docker-compose.prod.yml
sed -i "s/cpus: '0.75'/cpus: '1.50'/g" docker-compose.prod.yml
sed -i "s/cpus: '1.00'/cpus: '2.00'/g" docker-compose.prod.yml
sed -i "s/cpus: '0.50'/cpus: '1.00'/g" docker-compose.prod.yml
sed -i "s/cpus: '0.25'/cpus: '0.50'/g" docker-compose.prod.yml
sed -i "s/cpus: '0.10'/cpus: '0.25'/g" docker-compose.prod.yml

# 验证修改
grep -E "memory:|cpus:" docker-compose.prod.yml | sort -u
```

预期输出（8C16G）：
```
          cpus: '0.50'  # 小工具
          cpus: '1.00'  # 中等服务
          cpus: '1.50'  # 后端服务
          cpus: '2.00'  # 网关/工作流
          memory: 1G    # 后端服务
          memory: 1.5G  # 高负载服务
          memory: 3G    # MySQL/ES
```

---

## 6. 恢复 GELF 日志驱动

当前 `docker-compose.yml` 因 Docker Desktop DNS 问题临时改成了 `json-file`。
Ubuntu 服务器上 Docker 网络 DNS 正常工作，可以恢复 GELF：

```bash
# 将 logging driver 改回 gelf
sed -i 's/driver: json-file/driver: gelf/g' docker-compose.yml
sed -i 's/max-size: "10m"/gelf-address: "udp:\/\/platform-logstash:12201"/g' docker-compose.yml
sed -i '/max-file: "3"/d' docker-compose.yml
```

> 或者如果你不想用 ELK，保留 json-file 也可以，只是日志不会聚合到 Kibana。

---

## 7. 拉取镜像并启动

```bash
cd /opt/platform

# 拉取所有镜像（首次约 15-30 分钟，取决于网络）
docker compose \
  -f docker-compose.yml \
  -f docker-compose.ghcr.yml \
  -f docker-compose.prod.yml \
  --env-file .env \
  pull

# 启动所有服务
docker compose \
  -f docker-compose.yml \
  -f docker-compose.ghcr.yml \
  -f docker-compose.prod.yml \
  --env-file .env \
  up -d

# 首次启动后，MySQL + Flyway 会自动建表（约 1-2 分钟）
# 查看启动进度
docker compose -f docker-compose.yml -f docker-compose.ghcr.yml -f docker-compose.prod.yml logs -f platform-user
```

---

## 8. 验证部署

### 8.1 所有容器状态

```bash
docker compose ps
```

期望：所有 17+ 个容器 `Up`，大部分 `healthy`

### 8.2 服务健康检查

```bash
# API 网关
curl -s http://localhost:8083/actuator/health | jq .

# Auth 登录
curl -s -X POST http://localhost:8083/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | jq .
```

### 8.3 内存确认

```bash
docker stats --no-stream
```

验证每个服务的内存不超过 prod.yml 设定的限制。

### 8.4 前端访问

| 服务 | 地址 | 默认账号 |
|------|------|---------|
| 管理后台 | `http://<服务器IP>:8080` | admin / 123456 |
| 运营后台 | `http://<服务器IP>:8090` | admin / 123456 |
| API 网关 | `http://<服务器IP>:8083` | - |
| Zipkin | `http://<服务器IP>:9411` | - |
| Kibana | `http://<服务器IP>:5601` | - |

---

## 9. 后续更新

```bash
cd /opt/platform

# 1. 拉最新代码
git pull

# 2. 拉新镜像
docker compose -f docker-compose.yml -f docker-compose.ghcr.yml -f docker-compose.prod.yml pull

# 3. 重启变更的服务
docker compose -f docker-compose.yml -f docker-compose.ghcr.yml -f docker-compose.prod.yml up -d
```

---

## 10. 常见问题

### Q: 镜像拉取慢 / 超时
用 `docker pull ghcr.io/fkeu520/cloudplatform/platform-user:latest` 单独拉，
观察进度，超时重试即可。

### Q: 服务启动后 unhealthy
`docker inspect <service>` 看健康检查日志。通常是 Flyway 建表未完成（等 1-2 分钟即可）。

### Q: 登录返回 401
确认 JWT_SECRET 与开发环境一致（当前硬编码，后续改环境变量）。

### Q: 想关闭某些服务节省资源
临时方式：
```bash
docker compose stop kafka-ui kibana xxl-job-admin
```
持久方式：在 `docker-compose.prod.yml` 对应 service 加 `profiles: ["minimal"]`，启动时 `--profile ""` 排除。
