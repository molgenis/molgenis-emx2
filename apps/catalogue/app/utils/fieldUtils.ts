export const isEmpty: (value: any) => boolean = (value) => {
  return (
    value === null ||
    value === undefined ||
    (typeof value === "object" && Object.keys(value).length === 0)
  );
};
