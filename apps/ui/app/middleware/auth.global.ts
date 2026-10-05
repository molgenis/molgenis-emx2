import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { sanitizeRedirectPath } from "../../../tailwind-components/app/utils/redirect";

export default defineNuxtRouteMiddleware((to, from) => {
  // handle redirect to login with schema
  if (to.path === "/login" && from.params.schema && !to.query.schema) {
    return navigateTo({
      path: "/login",
      replace: true,
      query: {
        schema: from.params.schema,
        redirectTo: sanitizeRedirectPath(from.path),
      },
    });
  }

  // handle redirect to login without schema
  if (to.path === "/login" && !from.params.schema && !to.query.redirectTo) {
    return navigateTo({
      path: "/login",
      replace: true,
      query: {
        redirectTo: sanitizeRedirectPath(from.path),
      },
    });
  }
});
