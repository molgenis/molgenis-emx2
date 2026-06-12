import { useRoute } from "#app";
import type { Crumb } from "../../../tailwind-components/types/types";

export function useResourceDetailsCrumbs() {
  const route = useRoute();
  const crumbs: Crumb[] = [];
  if (route.params.catalogue) {
    crumbs.push({
      label: route.params.catalogue as string,
      url: `/${route.params.catalogue}`,
    });
    if (route.params.resourceType !== "about")
      crumbs.push({
        label: route.params.resourceType as string,
        url: `/${route.params.catalogue}/${route.params.resourceType}`,
      });
    crumbs.push({
      label: route.params.resource as string,
      url: "",
    });
  } else {
    crumbs.push({
      label: "Home",
      url: `/`,
    });
    crumbs.push({
      label: "Browse",
      url: `/all`,
    });
    if (route.params.resourceType !== "about")
      crumbs.push({
        label: route.params.resourceType as string,
        url: `/all/${route.params.resourceType}`,
      });
  }

  return crumbs;
}
