module.exports = {
  publicPath: "", // to ensure relative paths are used
  devServer: {
    port: "9090",
    proxy: {
      "^/graphql": {
        target: "https://emx2-catalogue.test.molgenis.org/LifeCycle4",
      },
      "/api": { target: "https://emx2-catalogue.test.molgenis.org" },
      "/apps": { target: "https://emx2-catalogue.test.molgenis.org" },
      "^/theme.css": { target: "https://emx2-catalogue.test.molgenis.org/LifeCycle4" },
    },
  },
};
