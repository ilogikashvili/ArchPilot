# ArchPilot frontend

ArchPilot is a client-rendered React/TypeScript single-page application built
with Vite, TanStack Router, Tailwind CSS, and React Flow. Production output is
static files in `dist/`; it does not need a Node.js frontend server.

## Local development

```sh
npm ci
npm run dev
```

Vite serves the frontend on port 5173 by default and proxies `/api` and `/ws`
to `http://127.0.0.1:8081`. Override the backend target with `API_TARGET` and
the frontend port with `FRONTEND_PORT`.

## Build and deploy

```sh
npm ci
npm run build
```

The build creates `dist/index.html`, hashed bundles under `dist/assets/`, and
copies public files such as `favicon.ico` and `robots.txt`.

To deploy from the Hestia host, copy the build contents into the document root:

```sh
cp -a dist/. /home/techno/web/iliagikashvili.ge/public_html/
```

Or upload from a build machine (replace the host name):

```sh
rsync -av dist/ techno@HESTIA_HOST:/home/techno/web/iliagikashvili.ge/public_html/
```

These commands overlay the deployed files without deleting unrelated content
in `public_html`.

## API and WebSocket configuration

The production defaults are same-origin:

- REST API: `/api/v1` (all API requests remain under `/api`)
- Collaboration WebSockets: `/ws`; the client automatically uses `wss:` when
  the page is served over HTTPS

Set `VITE_API_BASE_URL` or `VITE_WS_BASE_URL` at build time only when the
backend is hosted at a different public base URL. These are public browser
configuration values, not a place for credentials or secrets. A cross-origin
API host must allow the website origin through CORS. `API_TARGET` configures
only Vite's local development proxy; it has no effect on the production
deployment.

Provider API keys are submitted to Spring Boot and are stored/used by the
backend. No privileged backend credentials are embedded in the frontend.

## Hestia/Nginx configuration

Add the API and WebSocket locations to the HTTPS virtual host for
`iliagikashvili.ge`, retaining Hestia's existing TLS and certificate
directives. Confirm the document root matches the deployed directory.
`proxy_pass` has no URI suffix so the original `/api` and `/ws` paths reach
Spring Boot unchanged. Set `root` and `index` at server scope (or retain their
existing correct values), and replace the existing catch-all `location /`
with the SPA fallback below rather than creating a duplicate location.

```nginx
root /home/techno/web/iliagikashvili.ge/public_html;
index index.html;

location = /api {
    proxy_pass http://127.0.0.1:8081;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 900s;
    proxy_send_timeout 900s;
}

location ^~ /api/ {
    proxy_pass http://127.0.0.1:8081;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 900s;
    proxy_send_timeout 900s;
}

location = /ws {
    proxy_pass http://127.0.0.1:8081;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 3600s;
}

location ^~ /ws/ {
    proxy_pass http://127.0.0.1:8081;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 3600s;
}

location / {
    try_files $uri $uri/ /index.html;
}
```

API paths are handled by the proxy locations before the SPA fallback. Unknown
API endpoints therefore receive the backend's response instead of `index.html`.
The fallback serves `index.html` for application routes such as
`/projects/project-123`, allowing direct navigation and refresh.
After updating the Hestia-managed configuration, validate and reload Nginx
using the host's standard workflow (for example, `sudo nginx -t` followed by
`sudo systemctl reload nginx`).

## Rendering and limitations

The source routes are client-rendered and do not define route loaders,
actions/server functions, or middleware that require TanStack Start. REST
authentication, authorization, and privileged operations remain on the
Spring Boot backend. The frontend's route guards are only a UI convenience;
the backend must enforce access control for every protected API.

The editor's current mock data and other pre-existing application/API
limitations are unchanged. The Ollama URL must be reachable by the backend,
not merely by the browser.
