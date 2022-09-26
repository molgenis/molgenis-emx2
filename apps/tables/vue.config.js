module.exports = {
  runtimeCompiler: true,
  publicPath: "", // to ensure relative paths are used
  devServer: {
    port: "9090",
    proxy: require("../dev-proxy.config"),
  },
};
