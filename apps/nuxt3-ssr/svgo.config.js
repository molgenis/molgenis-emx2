/* eslint-disable no-undef */

module.exports = {
  //format for latest svgo:
  plugins: [
    {
      name: "convertColors",
      params: {
        currentColor: true,
      },
    },
    {
      name: "removeDimensions",
      params: {
        currentColor: true,
      },
    },
  ],
};
