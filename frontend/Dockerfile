# ─────────────────────────────────────────────
# Etapa 1: Build con Node 20
# ─────────────────────────────────────────────
FROM node:20-alpine AS build

WORKDIR /app

# Instalar dependencias
COPY package*.json ./
RUN npm ci --silent

# Copiar fuentes y construir el bundle de producción
COPY . .

# VITE_API_URL apunta al backend dentro de Docker (se puede sobreescribir con --build-arg)
ARG VITE_API_URL=http://localhost:8080/v1
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# ─────────────────────────────────────────────
# Etapa 2: Servir con Nginx Alpine
# ─────────────────────────────────────────────
FROM nginx:1.27-alpine

# Copiar el build estático al directorio de Nginx
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar configuración personalizada (soporte SPA / React Router)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
