import type { columnValue } from "../../metadata-utils/src/types";

export const toFormData = (rowData: Record<string, columnValue>) => {
  if (!FormData) {
    throw "Files can only be uploaded via a browser client";
  }
  const formData = new FormData();
  const nonFileValue: Record<string, columnValue> = {};
  const fileValues: Record<string, File> = {};

  // split into file and non-file entries
  for (const [key, value] of Object.entries(rowData)) {
    isFileValue(value)
      ? (fileValues[key] = value)
      : (nonFileValue[key] = value);
  }

  // add the file objects to the formData and place a link to the object in the variables
  for (const [key, value] of Object.entries(fileValues)) {
    const id = Math.random().toString(36);
    formData.append(id, value);
    nonFileValue[key] = id;
  }

  formData.append("variables", JSON.stringify({ value: [nonFileValue] }));

  return formData;
};

const isFileValue = (value: File | any) => {
  if (window && "File" in window) {
    return value instanceof File;
  } else {
    throw "Files can only be uploaded via a browser client";
  }
};
