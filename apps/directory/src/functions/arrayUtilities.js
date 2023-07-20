/**
 *
 * @param {*} fullArray the array which you want to divide.
 * @param {*} chunkSize the amount per array in the array
 *
 * returns for given input: ([11], 5): [[5],[5],[1]]
 */
export const convertArrayToChunks = function (fullArray, chunkSize = 100) {
  let chunkArray = [];
  const arrayLength = fullArray.length;

  let chunk = [];
  if (fullArray.length > 20) {
    for (let index = 0; index < arrayLength; index++) {
      chunk.push(fullArray[index]);
      if (chunk.length === chunkSize) {
        chunkArray.push(chunk);
        chunk = [];
      }
    }
    if (chunk.length) {
      chunkArray.push(chunk);
      chunk = [];
    }
  } else {
    chunkArray = [fullArray];
  }

  return chunkArray;
};
