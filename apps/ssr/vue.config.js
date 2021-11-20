module.exports = {
  publicPath: "", // to ensure relative paths are used
  devServer: {
    port: "9090",
    proxy: require("../dev-proxy.config"),
  },
  configureWebpack: {
    output: {
      filename: "[name].js",
      chunkFilename: "[name].js",
    },
  },
};
