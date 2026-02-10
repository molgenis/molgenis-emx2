import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { RESERVED_ROUTES } from "../utils/constants";

export default defineNuxtRouteMiddleware((to) => {
  const pathSegments = to.path.split("/").filter(Boolean);

  if (pathSegments.length !== 3) {
    return;
  }

  const [first, resourceType, resourceId] = pathSegments;

  if (RESERVED_ROUTES.includes(first)) {
    return;
  }

  if (resourceType === "collections" || resourceType === "networks") {
    return navigateTo(`/${resourceId}?catalogue=${first}`, {
      redirectCode: 301,
    });
  }
});
