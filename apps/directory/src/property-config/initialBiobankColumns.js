export const initialBiobankColumns = [
  { label: "Id:", column: "id", type: "string", showCopyIcon: true },
  {
    label: "PID:",
    column: "pid",
    type: "string",
    showCopyIcon: true,
    copyValuePrefix: "http://hdl.handle.net/",
  },
  { label: "Description:", column: "description", type: "longtext" },
  {
    label: "Quality labels:",
    column: {
      quality: [{ quality_standard: ["name"] }],
    },
    type: "quality",
    showOnBiobankCard: true,
  },
  {
    label: "Collection types:",
    column: { collections: [{ type: ["name", "label"] }] },
    type: "array",
    showOnBiobankCard: true,
  },
  {
    label: "Juridical person:",
    column: "juridical_person",
    type: "string",
    showOnBiobankCard: true,
  },
  {
    label: "Biobank capabilities:",
    column: { capabilities: ["name", "label"] },
    type: "array",
    showOnBiobankCard: true,
  },
  /** properties that are required but should not be rendered as attributes */
  {
    column: [
      "name",
      "head.title_before_name",
      "head.first_name",
      "head.last_name",
      "head.role",
      "contact.first_name",
      "contact.last_name",
      "contact.country.name",
      "contact.country.label",
      "country.label",
      "network.name",
      "network.id",
      "url",
      "withdrawn",
      "collections.materials.name",
      "also_known.url",
      "also_known.name_system",
    ],
  },
];

export default initialBiobankColumns;
