# freepark-cloud-simple-frontend-mnt

Vue 3 admin console for FreePark Cloud Simple. Talks to [`freepark-cloud-simple-backend`](../freepark-cloud-simple-backend) (JDK 21, default port `8080`).

FreePark 云平台管理后台，基于 Vue 3 + TypeScript + Vite，对接云端后端。

## Stack

- Vue 3 + Vite + TypeScript
- Vue Router + Pinia
- Element Plus
- vue-i18n

## Setup

Requires Node.js 22+. Start the backend (JDK 21) first.

```sh
npm install
npm run dev
```

Dev server: [http://localhost:5173](http://localhost:5173)

Vite proxies `/api` to `http://127.0.0.1:8080` (`VITE_DEV_PROXY_TARGET`).

First-run default account:

- username: `admin`
- password: `admin123`

## Scripts

```sh
npm run dev
npm run build
```
