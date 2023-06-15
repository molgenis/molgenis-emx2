const initialCollectionColumns = [
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
    column: "order_of_magnitude",
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
    column: "order_of_magnitude_donors",
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
    label: "Age:",
    type: "range",
    min: "age_low",
    max: "age_high",
    unit: "age_unit",
  },
  { label: "Type:", column: "type", type: "mref", showOnBiobankCard: true },
  { label: "Sex:", column: "sex", type: "categoricalmref" },
  {
    label: "Materials:",
    column: "materials",
    type: "categoricalmref",
    showOnBiobankCard: true,
  },
  {
    label: "Storage:",
    column: "storage_temperatures",
    type: "categoricalmref",
  },
  { label: "Data:", column: "data_categories", type: "categoricalmref" },
  {
    label: "Diagnosis:",
    column: "diagnosis_available",
    type: "mref",
    rsql: "diagnosis_available(label,uri,code)",
  },
  {
    label: "Data use conditions:",
    column: "data_use",
    type: "mref",
    rsql: "data_use(label,uri)",
  },
];

export default initialCollectionColumns;
