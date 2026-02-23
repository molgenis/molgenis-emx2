import { useRoute } from "#app";
import { computed } from "vue";
import type { Crumb } from "../../../tailwind-components/types/types";
import {
  getCatalogueId,
  buildResourceUrl,
  buildCatalogueBreadcrumbs,
} from "../utils/catalogueContextUtils";

export const useCatalogueContext = () => {
  const route = useRoute();

  const catalogueId = computed(() => {
    return getCatalogueId(
      route.query.catalogue as string | undefined,
      route.params.resourceId as string | undefined,
      route.path
    );
  });

  const isCatalogueScoped = computed(() => catalogueId.value !== null);

  const resourceUrl = (path: string) => {
    return buildResourceUrl(path, catalogueId.value);
  };

  const buildBreadcrumbs = (items: Crumb[]): Crumb[] => {
    return buildCatalogueBreadcrumbs(items, catalogueId.value);
  };

  return {
    catalogueId,
    isCatalogueScoped,
    resourceUrl,
    buildBreadcrumbs,
  };
};
