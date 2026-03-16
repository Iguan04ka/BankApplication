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
# Удаляем дефолтный конфиг и добавляем конфиг для SPA и проксирования /statement к gateway
RUN rm /etc/nginx/conf.d/default.conf \
 && cat > /etc/nginx/conf.d/default.conf <<'NGINX_CONF'
server {
  listen 80;
  server_name localhost;
  root /usr/share/nginx/html;
  index index.html;

  # Прокси для запроса формы к backend-сервису в docker-compose
  location = /statement {
    proxy_pass http://gateway:8085/statement;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
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