export const useQueryParams = () => {
  const route = useRoute();
  const keys = route.query.keys;
  if (typeof keys !== "string") {
    throw new Error("invalid record identifier");
  }
  return { key: transformToKeyObject(keys) };
};
