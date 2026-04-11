type RouteQuery = Record<
  string,
  string | string[] | (string | null)[] | null | undefined
>;

export function resolveRouteRouter(provided?: {
  route?: { query: RouteQuery } | null;
  router?: { replace: (opts: Record<string, unknown>) => void } | null;
}): {
  route: { query: RouteQuery } | null;
  router: { replace: (opts: Record<string, unknown>) => void } | null;
} {
  if (provided?.route && provided?.router) {
    return { route: provided.route, router: provided.router };
  }
  try {
    const { useRoute, useRouter } = require("#app/composables/router");
    return { route: useRoute(), router: useRouter() };
  } catch {
    return { route: null, router: null };
  }
}
