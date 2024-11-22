import type { ICranioSchemas } from ".";
import type { IOrganisations } from "./schema";

export interface IAppPage {
  organisation: IOrganisations;
  schemaNames: ICranioSchemas;
}
