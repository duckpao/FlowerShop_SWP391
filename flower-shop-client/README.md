# FlowerShop client (React + Vite)

Frontend of FlowerShop. Source is JSX (`src/**/*.jsx`) plus a few `.tsx` and `.js` files; the Vite config is `vite.config.js`.

## Scripts (from `package.json`)

| Command | What it does |
|---|---|
| `npm install` | Install dependencies (Gradle runs this too, as task `installFrontend`) |
| `npm run dev` | Vite dev server on the default port 5173; `/api` is proxied to `http://localhost:8080` |
| `npm run build` | Production build to `dist/` (Gradle task `buildFrontend` packages it into the backend) |
| `npm run preview` | Serve the built `dist/` locally |
| `npm run lint` | Run `oxlint` (no `.oxlintrc.json` is committed; default rules) |
| `npm run backend` | Runs `scripts/start-backend.mjs` to start the Spring Boot backend |

In normal use the backend on port 8080 serves the built frontend, so `npm run dev` is only needed for frontend work.
