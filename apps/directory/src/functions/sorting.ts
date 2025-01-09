export const sortCollectionsByName = function (
  collectionArray: { name?: string }[]
) {
  const shallowCopy = [...collectionArray];

  return shallowCopy.sort((a, b) => {
    if (!a.name || !b.name) return 0;

    if (a.name.toLowerCase() < b.name.toLowerCase()) {
      return -1;
    }
    if (a.name.toLowerCase() > b.name.toLowerCase()) {
      return 1;
    }
    return 0;
  });
};
