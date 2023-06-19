const initialCollectionColumns = [
  { label: "Name:", column: "name", type: "string" },
  { label: "Id:", column: "id", type: "string", showCopyIcon: true },
  { label: "Website:", column: "url", type: "hyperlink" },
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
    type: "quality",
    showOnBiobankCard: true,
  },
  {
    label: "Size:",
    column: { order_of_magnitude: ["label"] },
    type: "object",
    property: "size",
  },
  {
    label: "Available:",
    column: "size",
    type: "int",
    suffix: "samples",
    showOnBiobankCard: true,
  },
  {
    label: "Donor size:",
    column: { order_of_magnitude_donors: ["label"] },
    type: "object",
    property: "size",
  },
  {
    label: "Donors:",
    column: "number_of_donors",
    type: "int",
    suffix: "donors",
  },
  {
    label: "Age low:",
    type: "int",
    column: "age_low"
  },
  {
    label: "Age high:",
    type: "int",
    column: "age_high"
  },
  {
    label: "Age unit:",
    type: "mref",
    column: { age_unit: ["label"] }
  },
  { label: "Type:", column: { type: ["label"] }, type: "array", showOnBiobankCard: true },
  { label: "Sex:", column: { sex: ["label"] }, type: "array" },
  {
    label: "Materials:",
    column: { materials: ["label"] },
    type: "array",
    showOnBiobankCard: true,
  },
  {
    label: "Storage:",
    column: { storage_temperatures: ["label"] },
    type: "categoricalmref",
  },
  { label: "Data:", column: { data_categories: ["label"] }, type: "array" },
  {
    label: "Diagnosis:",
    column: { diagnosis_available: ["label"] },
    type: "mref",
    rsql: "diagnosis_available(label,uri,code)",
  },
  {
    label: "Data use conditions:",
    column: { data_use: ["label", "ontologyTermURI"] },
    type: "array",
  },
];

export default initialCollectionColumns;
