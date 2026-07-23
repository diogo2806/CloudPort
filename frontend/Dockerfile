# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS build

WORKDIR /app

COPY cloudport/package*.json ./
# O cache identificado é reutilizado entre builds sem ser incorporado à imagem.
# npm ci mantém o build determinístico a partir do package-lock.json.
RUN --mount=type=cache,id=cloudport-npm-cache,target=/root/.npm,sharing=locked \
    npm ci --no-audit --no-fund

COPY cloudport/ ./
RUN npm run build

FROM nginx:1.27-alpine

LABEL org.opencontainers.image.title="CloudPort Frontend" \
      org.opencontainers.image.source="https://github.com/diogo2806/CloudPort"

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.d/40-cloudport-runtime-config.sh /docker-entrypoint.d/40-cloudport-runtime-config.sh
COPY --from=build /app/dist/cloudport/ /usr/share/nginx/html/

RUN chmod +x /docker-entrypoint.d/40-cloudport-runtime-config.sh \
    && find /usr/share/nginx/html -type d -exec chmod 0755 {} \; \
    && find /usr/share/nginx/html -type f -exec chmod 0644 {} \;

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/health >/dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]