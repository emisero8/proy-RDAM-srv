FROM node:20-alpine

WORKDIR /app

# Instalar solo dependencias de producción
COPY package*.json ./
RUN npm ci --only=production --silent

# Copiar código de la pasarela y módulo crypto
COPY server.js .
COPY crypto.js .

EXPOSE 3000

CMD ["node", "server.js"]
