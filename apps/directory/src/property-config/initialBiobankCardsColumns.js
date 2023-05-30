export const initialBiobankCardColumns = [
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
  },
  {
    label: "Collection types:",
    column: {
      collections: [
        "id",
        "size",
        { collectionType: ["label"] },
        {
          qualityInfo: [
            "label",
            "certificationReport",
            "certificationImage",
            "certificationNumber",
          ],
        },
        { subcollections: ["id"] },
      ],
    },
  },
  { label: "Juridical person:", column: "juridicalPerson" },
  { label: "Biobank capabilities:", column: { capabilities: ["label"] } },
];

export default initialBiobankCardColumns;
