export const emxTypes = {
  continuous: ["INT", "DECIMAL"],
  categorical: ["STRING", "REF", "ONTOLOGY"],
};

export function emxTypeAsDataClass(value: string): string {
  return Object.keys(emxTypes)
    .map((key: string) => {
      if (emxTypes[key as string].includes(value)) {
        return key;
      }
    })
    .filter((val: string) => val)[0];
}
