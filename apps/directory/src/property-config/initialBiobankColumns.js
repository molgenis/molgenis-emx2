export const initialBiobankColumns = [
  { label: "Id:", column: "id", type: "string", showCopyIcon: true },
  {
    label: "PID:",
    column: "PID",
    type: "string",
    showCopyIcon: true,
    copyValuePrefix: "http://hdl.handle.net/",
  },
  { label: "Description:", column: "description", type: "longtext" },
  {
    label: "Quality labels:",
    column: {
      qualityInfo: [
        "label",
        "certificationReport",
        "certificationImage",
        "certificationNumber",
      ],
    },
    type: "quality",
    showOnBiobankCard: true,
  },
  {
    label: "Collection types:",
    column: { collections: [{ collectionType: ["name", "label"] }] },
    type: "array",
    showOnBiobankCard: true,
  },
  {
    label: "Juridical person:",
    column: "juridicalPerson",
    type: "string",
    showOnBiobankCard: true,
  },
  {
    label: "Biobank capabilities:",
    column: { capabilities: ["name", "label"] },
    type: "mref",
    showOnBiobankCard: true,
  },
];

export default initialBiobankColumns;
