FROM nginx:1.30.7-alpine
COPY docker/nginx.conf /etc/nginx/nginx.conf
COPY dist/compara-preco-web/browser/ /usr/share/nginx/html/
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --retries=6 CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
ENTRYPOINT ["nginx", "-g", "daemon off;"]
