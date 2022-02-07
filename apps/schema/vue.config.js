module.exports = {
  publicPath: "", // to ensure relative paths are used
  devServer: {
    port: "9092",
    proxy: require("../dev-proxy.config"),
  },
  configureWebpack: {
    devtool: "source-map",
  },
};
