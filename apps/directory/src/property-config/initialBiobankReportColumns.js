export const initialBiobankReportColumns = [
  {
    label: "Quality labels:",
    column: {
      quality: [
        "label",
        "certification_report",
        "certification_image_link",
        "certification_number",
      ],
    },
  },
  {
    label: "Collection types:",
    column: {
      collections: [
        "id",
        "size",
        { type: ["label"] },
        {
          quality: [
            "label",
            "certification_report",
            "certification_image_link",
            "certification_number",
          ],
        },
        { sub_collections: ["id"] },
      ],
    },
  },
  { label: "Juridical person:", column: "juridical_person" },
  { label: "Biobank capabilities:", column: { capabilities: ["label"] } },
  /** properties that are required but should not be rendered as attributes */
  {
    column: [
      "id",
      "name",
      "withdrawn",
      "collections.id",
      "collections.name",
      "collections.size",
      "collections.withdrawn",
      "collections.sub_collections.withdrawn",
    ],
  },
];

export default initialBiobankReportColumns;
