FROM node:18-alpine AS builder
WORKDIR /app
ENV PATH /app/node_modules/.bin:$PATH

# Сначала копируем package-файлы и устанавливаем зависимости (быстрая пересборка при изменении кода)
COPY package*.json ./
RUN npm ci --silent

# Копируем исходники и собираем production-версию
COPY . .
RUN npm run build

# Финальный образ на nginx для отдачи статических файлов
FROM nginx:alpine
# Удаляем дефолтный конфиг и добавляем простой и безопасный конфиг для SPA и проксирования API к gateway
RUN rm /etc/nginx/conf.d/default.conf \
 && cat > /etc/nginx/conf.d/default.conf <<'NGINX_CONF'
server {
  listen 80;
  server_name localhost;
  root /usr/share/nginx/html;
  index index.html;

  # Явно отдаём SPA по /statement
  location = /statement {
    try_files /index.html =404;
  }
  location ^~ /statement/ {
    try_files /index.html =404;
  }

  # Явно отдаём SPA на GET /auth/login, /auth/register, /auth/forgot-password
  location = /auth/login { try_files /index.html =404; }
  location = /auth/register { try_files /index.html =404; }
  location = /auth/forgot-password { try_files /index.html =404; }

  # GET /auth/reset-password/{token} (без trailing /valid) — это SPA-маршрут
  # Регулярка: token = один сегмент без '/'. /auth/reset-password/{token}/valid сюда НЕ попадает
  # и спустится ниже к прокси на gateway.
  location ~ ^/auth/reset-password/[^/]+$ {
    try_files /index.html =404;
  }

  # Прокси для API: /api/* -> gateway, убираем префикс /api
  location /api/ {
    rewrite ^/api/(.*)$ /$1 break;
    proxy_pass http://gateway:8085;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Authorization $http_authorization;
    proxy_http_version 1.1;
    proxy_set_header Connection "";
  }

  # Прокси для auth endpoints (все методы) — backend сам обрабатывает GET/POST
  # SPA-страницы (login/register/forgot-password/reset-password/{token}) уже отловлены выше.
  location /auth/ {
    proxy_pass http://gateway:8085;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Authorization $http_authorization;
    proxy_http_version 1.1;
    proxy_set_header Connection "";
  }

  # Всё прочее — отдаём SPA
  location / {
    try_files $uri $uri/ /index.html;
  }

  location /static/ {
    expires 1y;
    add_header Cache-Control "public, must-revalidate";
  }
}
NGINX_CONF

# Копируем собранный билд
COPY --from=builder /app/build /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
