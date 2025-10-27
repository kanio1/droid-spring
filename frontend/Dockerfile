# syntax=docker/dockerfile:1.6

ARG NODE_VERSION=22.11.0

FROM node:${NODE_VERSION}-alpine AS builder

WORKDIR /app

ENV PNPM_HOME="/usr/local/share/pnpm"
ENV PATH="$PNPM_HOME:$PATH"

RUN corepack enable pnpm

COPY pnpm-workspace.yaml ./
COPY package.json pnpm-lock.yaml ./

COPY .npmrc .npmrc

RUN pnpm install --frozen-lockfile

COPY . .

RUN pnpm run build

FROM node:${NODE_VERSION}-alpine AS runtime

WORKDIR /app

ENV NODE_ENV=production
ENV NITRO_HOST=0.0.0.0
ENV NITRO_PORT=3000
ENV PORT=3000

COPY --from=builder /app/.output ./.output
COPY --from=builder /app/package.json ./package.json

EXPOSE 3000

CMD ["node", ".output/server/index.mjs"]
