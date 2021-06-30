const HOST = process.env.MOLGENIS_APPS_HOST || "https://emx2.dev.molgenis.org";
const SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "Catalogue_test";

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
