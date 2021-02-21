module.exports = {
  publicPath: "", // to ensure relative paths are used
  devServer: {
    port: "9092",
    proxy: {
      "^/graphql": {
        target: "http://localhost:8080/CohortsCentral",
      },
      "/apps": { target: "http://localhost:8080" },
    },
  },
  configureWebpack: {
    devtool: "source-map",
  },
};
