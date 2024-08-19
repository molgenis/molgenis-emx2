type ICollectionTypeMetadata = {
  type: string;
  plural: string;
  image?: string;
  path: string;
  description?: string;
};

export const typeMetadata: ICollectionTypeMetadata[] = [
  {
    type: "Cohort study",
    plural: "Cohort studies",
    image: "image-link",
    path: "cohorts",
    description: "Cohorts & Biobanks",
  },
  {
    type: "Data source",
    plural: "Data sources",
    image: "image-data-warehouse",
    path: "datasources",
    description: "Integration of multiple databanks",
  },
  {
    type: "Databank",
    plural: "Databanks",
    path: "databanks",
    description: "Databanks & Registries",
  },
  {
    type: "Biobank",
    plural: "Biobanks",
    path: "biobanks",
    description: "Biobanks and sample collections",
  },
  {
    type: "Network",
    plural: "Networks",
    image: "image-diagram",
    path: "networks",
    description: "Networks & Consortia",
  },
  { type: "Study", plural: "Studies", path: "studies" },
];

export function getCollectionMetadataForType(
  type: string
): ICollectionTypeMetadata {
  return (
    Object.values(typeMetadata).filter(
      (value: ICollectionTypeMetadata) => value.type === type
    )?.[0] || {
      type: "Collection",
      plural: "Collections",
      image: "image-link",
      path: "collections",
    }
  );
}

export function getCollectionMetadataForPath(
  path: string
): ICollectionTypeMetadata {
  return (
    Object.values(typeMetadata).filter(
      (value: ICollectionTypeMetadata) => value.path === path
    )?.[0] || {
      type: "Collection",
      plural: "Collections",
      image: "image-link",
      path: "collections",
    }
  );
}
