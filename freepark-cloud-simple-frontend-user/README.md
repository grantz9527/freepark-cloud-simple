# freepark-cloud-simple-frontend-user

Vue 3 driver-facing portal for FreePark Cloud Simple (plate lookup, fees, pay). Talks to [`freepark-cloud-simple-backend`](../freepark-cloud-simple-backend) (JDK 21, default port `8080`).

FreePark 云平台用户端（查费 / 缴费），基于 Vue 3 + TypeScript + Vite，对接云端后端。

## Stack

- Vue 3 + Vite + TypeScript
- Vue Router

## Setup

Requires Node.js 22+. Start the backend (JDK 21) first.

```sh
npm install
npm run dev
```

Dev server: [http://localhost:5174](http://localhost:5174)

Vite proxies `/api` to `http://127.0.0.1:8080` (`VITE_DEV_PROXY_TARGET`).

## Scripts

```sh
npm run dev
npm run build
```
