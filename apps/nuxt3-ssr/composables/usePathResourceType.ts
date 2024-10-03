import type { IResourceTypeMetadata } from "~/interfaces/types";

export const usePathResourceType = () => {
  const route = useRoute();
  const resourceType = route.params.resourceType;
  return (
    Object.values(typeMetadata).filter(
      (value: IResourceTypeMetadata) => value.path === resourceType
    )?.[0] || {
      type: "Resource",
      plural: "Resources",
      image: "image-link",
      path: "resources",
    }
  );
};
