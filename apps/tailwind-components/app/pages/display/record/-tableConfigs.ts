import type { TableConfig } from "../../../../types/recordConfig";

export const tableConfigs: Record<string, TableConfig> = {
  Collections: {
    columns: {
      subpopulations: { route: "subpopulations" },
      collectionEvents: { route: "collection-events" },
      tables: {
        display: {
          layout: "TABLE",
          titleColumnId: "name",
          detailColumnIds: ["description", "numberOfRows"],
        },
      },
      organisationsInvolved: {
        display: { layout: "LINKS", titleColumnId: "name" },
      },
      peopleInvolved: {
        display: {
          layout: "LIST",
          titleColumnId: "displayName",
          detailColumnIds: ["role", "email"],
        },
      },
      partOfNetworks: {
        display: {
          layout: "CARDS",
          titleColumnId: "name",
          descriptionColumnId: "description",
          logoColumnId: "logo",
        },
      },
      publications: { display: { layout: "BULLETS", titleColumnId: "title" } },
      documentation: { display: { layout: "LINKS", titleColumnId: "name" } },
    },
  },
  Subpopulations: {
    display: {
      layout: "CARDS",
      titleColumnId: "name",
      descriptionColumnId: "description",
      detailColumnIds: ["inclusionStart", "inclusionEnd"],
    },
  },
  CollectionEvents: {
    display: {
      layout: "TABLE",
      titleColumnId: "name",
      detailColumnIds: ["numberOfParticipants", "startDate", "endDate"],
    },
    columns: { subpopulations: {} },
  },
};
