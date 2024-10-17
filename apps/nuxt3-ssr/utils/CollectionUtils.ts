import type { IResourceTypeMetadata } from "~/interfaces/types";

export const typeMetadata: IResourceTypeMetadata[] = [
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
  {
    type: "Clinical trial",
    plural: "Clinical Trials",
    path: "trials",
    image: "image-data-warehouse",
    description: "Prospective collection with intervention(s)",
  },
  {
    type: "Common data model",
    plural: "Common data models",
    path: "cdms",
    image: "image-data-warehouse",
    description: "For data harmonization",
  },
  {
    type: "Other type",
    plural: "Other types",
    path: "other-types",
    image: "image-data-warehouse",
    description: "Other type of resource",
  },
];

export function getResourceMetadataForType(
  type: string
): IResourceTypeMetadata {
  return (
    Object.values(typeMetadata).filter(
      (value: IResourceTypeMetadata) => value.type === type
    )?.[0] || {
      type: type,
      plural: type,
      image: "image-link",
      path: type,
      description: type,
    }
  );
}
