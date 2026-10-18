import tailwindcss from "@tailwindcss/vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import react from "@vitejs/plugin-react";
import { defineConfig, type ProxyOptions } from "vite";

const apiTarget = process.env["API_TARGET"] || "http://127.0.0.1:8081";
const longRequestTimeout = 15 * 60 * 1000;

const quietProxyErrors: NonNullable<ProxyOptions["configure"]> = (proxy) => {
  proxy.on("error", (error, _request, response) => {
    const code = "code" in error ? error.code : undefined;
    if (code === "ECONNRESET" || code === "EPIPE") return;
    if ("writeHead" in response && !response.headersSent) {
      response.writeHead(503, { "Content-Type": "application/json" });
      response.end(
        JSON.stringify({
          status: 503,
          title: `ArchMain backend is not running on ${apiTarget}. Start it with ./gradlew bootRun`,
        }),
      );
    } else if ("destroy" in response) {
      response?.destroy?.();
    }
  });
  proxy.on("proxyReqWs", (_proxyRequest, _request, socket) =>
    socket.on("error", () => {}),
  );
};

export default defineConfig({
  plugins: [
    tanstackRouter({ target: "react", autoCodeSplitting: true }),
    react(),
    tailwindcss(),
  ],
  server: {
    port: Number(process.env["FRONTEND_PORT"] || 5173),
    strictPort: true,
    proxy: {
      "/api": {
        target: apiTarget,
        changeOrigin: true,
        timeout: longRequestTimeout,
        proxyTimeout: longRequestTimeout,
        configure: quietProxyErrors,
      },
      "/ws": {
        target: apiTarget.replace(/^http/, "ws"),
        ws: true,
        changeOrigin: true,
        configure: quietProxyErrors,
      },
    },
  },
  build: {
    outDir: "dist",
  },
  resolve: {
    tsconfigPaths: true,
  },
});
