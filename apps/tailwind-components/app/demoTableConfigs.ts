import type { TableConfig } from "../types/recordConfig";

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
        display: {
          layout: "CARDS",
          titleColumnId: "organisationName",
          logoColumnId: "logo",
          detailColumnIds: ["organisationWebsite"],
        },
      },
      peopleInvolved: {
        display: {
          layout: "CARDS",
          titleColumnId: "displayName",
          descriptionColumnId: "roleDescription",
          logoColumnId: "photo",
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
      layout: "TABLE",
      titleColumnId: "name",
      detailColumnIds: ["description", "numberOfParticipants"],
    },
  },
  CollectionEvents: {
    display: {
      layout: "TABLE",
      titleColumnId: "name",
      detailColumnIds: ["description", "numberOfParticipants"],
    },
    columns: { subpopulations: {} },
  },
};
