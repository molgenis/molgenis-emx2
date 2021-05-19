module.exports = {
  publicPath: "", // to ensure relative paths are used
  devServer: {
    port: "9092",
    proxy: {
      "^/graphql": {
        target: "http://localhost:8080/CohortNetwork",
      },
      "/apps": { target: "http://localhost:8080" },
      "^/theme.css": {
        target: "http://localhost:8080/pet store",
      },
    },
  },
  configureWebpack: {
    devtool: "source-map",
  },
};
