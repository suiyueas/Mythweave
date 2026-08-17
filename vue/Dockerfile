# ========================================================
# Mythweave AI 前端镜像（多阶段构建）
# 阶段一：Node 20 编译 Vue 3 产物
# 阶段二：Nginx 托管静态资源并反向代理后端 /api
# ========================================================
FROM node:20-alpine AS builder
WORKDIR /app

# 可在构建时覆盖：--build-arg VITE_API_BASE=http://127.0.0.1:8080
# 默认留空 = 同源请求，由 Nginx 将 /api 反代到后端容器
ARG VITE_API_BASE=""
ENV VITE_API_BASE=${VITE_API_BASE}
# 国内网络可传 --build-arg NPM_REGISTRY=https://registry.npmmirror.com 加速
ARG NPM_REGISTRY=https://registry.npmjs.org

COPY package.json package-lock.json ./
RUN npm ci --registry=${NPM_REGISTRY}

COPY . .
RUN npm run build

# ========================================================
# 运行阶段
# ========================================================
FROM nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=5 \
    CMD wget -q --spider http://127.0.0.1/ || exit 1
