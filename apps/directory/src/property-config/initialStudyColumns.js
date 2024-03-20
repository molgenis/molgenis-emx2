const initialStudyColumns = [
  { label: "Id:", column: "id", type: "string", showCopyIcon: true },
  { label: "Title:", column: "title", type: "string" },
  { label: "Description:", column: "description", type: "string" },
  { label: "Type:", column: "type", type: "string" },
  { label: "Sex:", column: { sex: ["label"] }, type: "array" },
  { label: "Number of subjects:", column: "number_of_subjects", type: "int" },
  {
    label: "Age:",
    type: "range",
    min: "age_high",
    max: "age_low",
    unit: "age_unit",
    unit_column: { age_unit: ["label"] },
  },
  {
    label: "Also Known In:",
    column: { also_known: ["name_system", "url"] },
    type: "xref",
  },
];

export default initialStudyColumns;
