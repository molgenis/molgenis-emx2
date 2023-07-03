export const sortCollectionsByName = function (collectionArray) {
  const newArray = [
    ...new Set(collectionArray),
  ]; /** remove the in place sorting */

  return newArray.sort((a, b) => {
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

export const sortCollectionsByLabel = function (collectionArray) {
  const newArray = [
    ...new Set(collectionArray),
  ]; /** remove the in place sorting */

  return newArray.sort((a, b) => {
    if (!a.label || !b.label) return 0;

    if (a.label.toLowerCase() < b.label.toLowerCase()) {
      return -1;
    }
    if (a.label.toLowerCase() > b.label.toLowerCase()) {
      return 1;
    }
    return 0;
  });
};
