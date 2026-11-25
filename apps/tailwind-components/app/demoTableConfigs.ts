import type { TableConfig } from "../types/recordConfig";

export const tableConfigs: Record<string, TableConfig> = {
  Collections: {
    columns: {
      subpopulations: { display: "TABLE", route: "subpopulations" },
      collectionEvents: { display: "TABLE", route: "collection-events" },
      tables: { display: "TABLE" },
      organisationsInvolved: { display: "CARDS" },
      peopleInvolved: { display: "CARDS" },
      partOfNetworks: { display: "CARDS" },
      publications: { display: "BULLETS" },
      documentation: { display: "LINKS" },
    },
  },
  CollectionEvents: {
    columns: {
      name: { role: "TITLE" },
      description: { role: "DETAIL" },
      numberOfParticipants: { role: "DETAIL" },
      subpopulations: { display: "TABLE" },
    },
  },
  Subpopulations: {
    columns: {
      name: { role: "TITLE" },
      description: { role: "DETAIL" },
      numberOfParticipants: { role: "DETAIL" },
    },
  },
  Tables: {
    columns: {
      name: { role: "TITLE" },
      description: { role: "DETAIL" },
      numberOfRows: { role: "DETAIL" },
    },
  },
  Organisations: {
    columns: {
      organisationName: { role: "TITLE" },
      logo: { role: "LOGO" },
      organisationWebsite: { role: "DETAIL" },
    },
  },
  Contacts: {
    columns: {
      displayName: { role: "TITLE" },
      roleDescription: { role: "DESCRIPTION" },
      photo: { role: "LOGO" },
      role: { role: "DETAIL" },
      email: { role: "DETAIL" },
    },
  },
  Networks: {
    columns: {
      name: { role: "TITLE" },
      description: { role: "DESCRIPTION" },
      logo: { role: "LOGO" },
    },
  },
  Publications: { columns: { title: { role: "TITLE" } } },
  Documentation: { columns: { name: { role: "TITLE" } } },
};
