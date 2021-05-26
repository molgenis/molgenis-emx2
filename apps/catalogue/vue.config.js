const HOST = process.env.MOLGENS_APPS_HOST || "http://localhost:8080";
const SCHEMA = process.env.MOLGENS_APPS_SCHEMA || "Conception";

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
    proxy: {
      "^/graphql": {
        target: `${HOST}/${SCHEMA}`,
      },
      "/api": { target: `${HOST}` },
      "/apps": { target: `${HOST}` },
      "^/theme.css": { target: `${HOST}/${SCHEMA}` },
    },
  },
};
