export const contactsMetadata = {
  name: "Contacts",
  tableType: "DATA",
  id: "Contacts",
  externalSchema: "Catalogue",
  columns: [
    {
      name: "resource",
      id: "resource",
      columnType: "REF",
      key: 1,
      refTable: "Resources",
      refLabelDefault: "${id}",
      required: true,
      descriptions: [
        {
          locale: "en",
          value: "Resource the contact is affiliated with",
        },
      ],
      position: 179,
    },
    {
      name: "first name",
      id: "firstName",
      columnType: "STRING",
      key: 1,
      required: true,
      descriptions: [
        {
          locale: "en",
          value: "First name of the contact person",
        },
      ],
      position: 182,
    },
    {
      name: "last name",
      id: "lastName",
      columnType: "STRING",
      key: 1,
      required: true,
      descriptions: [
        {
          locale: "en",
          value: "Last name of the contact person",
        },
      ],
      position: 183,
    },
    {
      name: "email",
      id: "email",
      columnType: "STRING",
      descriptions: [
        {
          locale: "en",
          value: "Contact's email address",
        },
      ],
      position: 188,
    },
    {
      name: "orcid",
      id: "orcid",
      columnType: "STRING",
      descriptions: [
        {
          locale: "en",
          value: "Orcid of the contact person",
        },
      ],
      position: 189,
    },
    {
      name: "mg_draft",
      id: "mg_draft",
      columnType: "BOOL",
      position: -5,
    },
  ],
};

export const resourcesMetadata = {
  name: "Resources",
  tableType: "DATA",
  id: "Resources",
  descriptions: [
    {
      locale: "en",
      value:
        "Generic listing of all resources. Should not be used directly, instead use specific types such as Databanks and Studies",
    },
  ],
  externalSchema: "Catalogue",
  columns: [
    {
      name: "id",
      id: "id",
      columnType: "STRING",
      key: 1,
      required: true,
      descriptions: [
        {
          locale: "en",
          value: "Internal identifier",
        },
      ],
      position: 1,
    },
    {
      name: "name",
      id: "name",
      columnType: "TEXT",
      key: 3,
      required: true,
      descriptions: [
        {
          locale: "en",
          value: "Name used in European projects",
        },
      ],
      position: 4,
    },
    {
      name: "description",
      id: "description",
      columnType: "TEXT",
      descriptions: [
        {
          locale: "en",
          value: "Short description",
        },
      ],
      position: 31,
    },
    {
      name: "mg_tableclass",
      id: "mg_tableclass",
      columnType: "STRING",
      readonly: true,
      position: 10005,
    },
  ],
};
