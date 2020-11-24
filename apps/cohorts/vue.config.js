module.exports = {
  publicPath: "", // to ensure relative paths are used
  devServer: {
    port: "9090",
    proxy: {
      "^/graphql": {
        target: "http://localhost:8080/CohortsSimple",
      },
      "/api": { target: "http://localhost:8080" },
      "/apps": { target: "http://localhost:8080" },
    },
  },
};
