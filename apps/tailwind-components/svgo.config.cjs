module.exports = {
  plugins: [
    {
      name: "removeUselessStrokeAndFill",
      params: {
        removeNone: true,
      },
    },
    {
      name: "removeAttrs",
      params: {
        attrs: ["*:fill:*"],
      },
    },
    {
      name: "addAttributesToSVGElement",
      params: {
        attributes: ['fill="currentColor"'],
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
