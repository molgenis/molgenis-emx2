import { useRoute } from "#app";
import { computed } from "vue";
import type { Crumb } from "../../../../tailwind-components/types/types";

export const useCatalogueContext = () => {
  const route = useRoute();

  const catalogueId = computed(() => {
    const queryParam = route.query.catalogue;
    if (typeof queryParam === "string") return queryParam;

    const resourceId = route.params.resourceId as string;
    if (!resourceId) return null;

    const pathAfterResource = route.path.slice(`/${resourceId}`.length);
    if (pathAfterResource.length > 1) {
      return resourceId;
    }

    return null;
  });

  const isCatalogueScoped = computed(() => catalogueId.value !== null);

  const resourceUrl = (path: string) => {
    const base = path.startsWith("/") ? path : `/${path}`;
    if (!catalogueId.value) {
      return base;
    }
    const pathResourceId = base.split("/")[1]?.split("?")[0];
    if (pathResourceId === catalogueId.value) {
      return base;
    }
    const separator = base.includes("?") ? "&" : "?";
    return `${base}${separator}catalogue=${catalogueId.value}`;
  };

  const buildBreadcrumbs = (items: Crumb[]): Crumb[] => {
    if (!catalogueId.value) {
      return items;
    }

    return [
      {
        label: catalogueId.value,
        url: resourceUrl(catalogueId.value),
      },
      ...items,
    ];
  };

  return {
    catalogueId,
    isCatalogueScoped,
    resourceUrl,
    buildBreadcrumbs,
  };
};
