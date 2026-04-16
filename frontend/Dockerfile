FROM node:22-alpine AS base
WORKDIR /app

# ─── Dev ────────────────────────────────────────────────────────────────────
FROM base AS dev

COPY package*.json ./
RUN npm install

COPY . .
EXPOSE 3000
CMD ["npm", "run", "dev"]

# ─── Build ──────────────────────────────────────────────────────────────────
FROM base AS builder

ARG NUXT_PUBLIC_API_URL
ENV NUXT_PUBLIC_API_URL=$NUXT_PUBLIC_API_URL

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# ─── Prod ───────────────────────────────────────────────────────────────────
FROM base AS prod

COPY --from=builder /app/.output /app/.output

ENV NODE_ENV=production
EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]
