import type { RecordListConfig } from "../../../../types/recordLists";

export const recordLists: Record<string, RecordListConfig> = {
  Collections: {
    subpopulations: {
      layout: "CARDS",
      titleColumn: "name",
      descriptionColumnId: "description",
      detailColumnIds: ["numberOfParticipants"],
    },
    collectionEvents: {
      layout: "TABLE",
      titleColumn: "name",
      detailColumnIds: ["numberOfParticipants", "startDate", "endDate"],
    },
    tables: {
      layout: "TABLE",
      titleColumn: "name",
      detailColumnIds: ["description", "numberOfRows"],
    },
    organisationsInvolved: { layout: "LINKS", titleColumn: "name" },
    peopleInvolved: {
      layout: "LIST",
      titleColumn: "displayName",
      detailColumnIds: ["role", "email"],
    },
    partOfNetworks: {
      layout: "CARDS",
      titleColumn: "name",
      descriptionColumnId: "description",
      logoColumnId: "logo",
    },
    publications: { layout: "BULLETS", titleColumn: "title" },
    documentation: { layout: "LINKS", titleColumn: "name" },
  },
};
