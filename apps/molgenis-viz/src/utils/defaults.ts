export const emxTypes = {
  continuous: ["INT", "DECIMAL"],
  categorical: ["STRING", "REF", "ONTOLOGY"],
};

export function emxTypeAsDataClass(value: String) {
  return Object.keys(emxTypes)
    .map((key: String) => {
      if (emxTypes[key as string].includes(value)) {
        return key;
      }
    })
    .filter((val: string) => val)[0];
}
