module.exports = {
  publicPath: "", // to ensure relative paths are used
  chainWebpack: (config) => {
    // GraphQL Loader, allows import of .gql files
    config.module
      .rule("graphql")
      .test(/\.(graphql|gql)$/)
      .use("webpack-graphql-loader")
      .loader("webpack-graphql-loader")
      .end();
  },
  devServer: {
    port: "9090",
    proxy: require("../dev-proxy.config"),
  },
};
