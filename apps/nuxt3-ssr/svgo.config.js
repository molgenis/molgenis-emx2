/* eslint-disable no-undef */
const { extendDefaultPlugins } = require("svgo");

module.exports = {
  plugins: extendDefaultPlugins([
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
  ]),
};
