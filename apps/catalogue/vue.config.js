module.exports = {
  publicPath: "", // to ensure relative paths are used
  devServer: {
    port: "9090",
    // proxy: {
    //   "^/graphql": {
    //     target: "http://localhost:8080/CohortsCentral",
    //   },
    //   "/api": { target: "http://localhost:8080" },
    //   "/apps": { target: "http://localhost:8080" },
    // },
    proxy: {
      '/graphql$': {
        target: 'https://emx2-catalogue.test.molgenis.org/CohortsCentral',
        ws: true,
        changeOrigin: true
      },
      "/api": { target: "https://emx2.test.molgenis.org" },
      "/apps": { target: "https://emx2.test.molgenis.org" },
    },
  },
};
