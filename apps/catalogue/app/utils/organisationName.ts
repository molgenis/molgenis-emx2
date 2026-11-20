import type { IOrganisationRoles } from "../../interfaces/catalogue";

export const organisationName = (o?: IOrganisationRoles | null) =>
  o?.organisationName || o?.organisation?.name || o?.organisationNotInROR;
