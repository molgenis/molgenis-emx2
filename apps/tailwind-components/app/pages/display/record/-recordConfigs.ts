import type { RecordConfig } from "../../../../types/recordConfig";

export const recordConfigs: Record<string, RecordConfig> = {
  Collections: {
    columns: {
      subpopulations: {
        display: {
          layout: "CARDS",
          titleColumn: "name",
          descriptionColumnId: "description",
          detailColumnIds: ["numberOfParticipants"],
        },
      },
      collectionEvents: {
        display: {
          layout: "TABLE",
          titleColumn: "name",
          detailColumnIds: ["numberOfParticipants", "startDate", "endDate"],
        },
      },
      tables: {
        display: {
          layout: "TABLE",
          titleColumn: "name",
          detailColumnIds: ["description", "numberOfRows"],
        },
      },
      organisationsInvolved: {
        display: { layout: "LINKS", titleColumn: "name" },
      },
      peopleInvolved: {
        display: {
          layout: "LIST",
          titleColumn: "displayName",
          detailColumnIds: ["role", "email"],
        },
      },
      partOfNetworks: {
        display: {
          layout: "CARDS",
          titleColumn: "name",
          descriptionColumnId: "description",
          logoColumnId: "logo",
        },
      },
      publications: { display: { layout: "BULLETS", titleColumn: "title" } },
      documentation: { display: { layout: "LINKS", titleColumn: "name" } },
    },
  },
};
