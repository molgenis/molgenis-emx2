// The backend rejects state-changing requests whose Origin is another host (CSRF protection).
// A dev server on localhost that proxies to e.g. emx2.dev.molgenis.org forwards the browser's
// Origin (http://localhost:<port>), so requests made by the app itself must be re-labelled with the
// origin of the proxy target. Requests coming from any other site keep their Origin and are still
// rejected by the backend.

function proxiedOrigin(headers, target) {
  const source = headers.origin || headers.referer;
  if (!source || !headers.host) return undefined;
  try {
    if (new URL(source).host !== headers.host) return undefined;
    return new URL(target).origin;
  } catch {
    return undefined;
  }
}

// for use as the `configure` option of a vite proxy entry
function rewriteOrigin(proxy, options) {
  proxy.on("proxyReq", (proxyReq, req) => {
    const origin = proxiedOrigin(req.headers, String(options.target));
    if (origin) proxyReq.setHeader("origin", origin);
  });
}

module.exports = { proxiedOrigin, rewriteOrigin };
