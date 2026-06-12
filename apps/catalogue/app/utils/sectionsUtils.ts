const DEFAULT_SECTION = {
  id: "default",
  label: "General Information",
  description: "",
  fields: [],
};
export const toSections = (data: any, metadata: any) => {
  const columns = metadata ? metadata.columns : [];
  return columns.reduce(
    (
      acc: any[],
      column: {
        columnType: string;
        id: string | number;
        label: any;
        description: any;
      }
    ) => {
      if (column.columnType === "HEADING") {
        acc.push({
          id: column.id,
          label: column.label,
          description: column.description,
          fields: [],
        });
      }
      if (column.columnType !== "HEADING") {
        if (!acc || acc.length === 0) {
          acc.push(DEFAULT_SECTION);
        }
        const currentHeading = acc[acc.length - 1];
        if (currentHeading) {
          currentHeading.fields.push({
            id: column.id,
            label: column.label,
            description: column.description,
            value: data ? data[column.id] : null,
          });
        }
      }
      return acc;
    },
    [] as any[]
  );
};
