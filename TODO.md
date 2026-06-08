# TODO - Fix Vite/Proxy login ECONNREFUSED

## Step 0 — Collect info
- [ ] Verify frontend is using `/api` via Vite proxy or direct `VITE_API_URL`.
- [ ] Verify backend Spring Boot is running on `server.port=8080`.

## Step 1 — Confirm failing request
- [ ] Check `frontend/src/api.js` and `frontend/vite.config.js` for `/api` base URL/proxy target.

## Step 2 — Fix configuration
- [ ] If backend not running: start Spring Boot.
- [ ] If ports mismatch: update Vite proxy target or `VITE_API_URL`.
- [ ] If path mismatch: ensure `AuthController` mapping `/api/auth/login` matches frontend request.

## Step 3 — Validate
- [ ] Re-run login from browser; confirm `/api/auth/login` responds.
- [ ] Ensure CORS/security permit `/api/auth/**`.

