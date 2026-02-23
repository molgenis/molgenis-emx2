import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { RESERVED_ROUTES } from "../utils/constants";

export default defineNuxtRouteMiddleware((to) => {
  const pathSegments = to.path.split("/").filter(Boolean);

  if (pathSegments.length !== 3) {
    return;
  }

  const first = pathSegments[0] as string;
  const resourceType = pathSegments[1] as string;
  const resourceId = pathSegments[2] as string;

  if (RESERVED_ROUTES.includes(first)) {
    return;
  }

  if (resourceType === "collections" || resourceType === "networks") {
    return navigateTo(`/${resourceId}?catalogue=${first}`, {
      redirectCode: 301,
    });
  }
});
