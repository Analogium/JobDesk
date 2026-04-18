# Official Playwright image — browsers pre-installed at /ms-playwright
FROM mcr.microsoft.com/playwright:v1.59.1-jammy

WORKDIR /app

COPY package*.json ./
RUN npm ci --omit=dev

COPY server.js .

EXPOSE 3001
CMD ["node", "server.js"]
