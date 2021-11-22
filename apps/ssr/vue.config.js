const nodeExternals = require("webpack-node-externals");
const ExtractCssChunksPlugin = require("extract-css-chunks-webpack-plugin");

const isProduction = process.env.NODE_ENV === "production";

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
    //https://ssr.vuejs.org/guide/build-config.html#server-config
    target: isProduction ? "node" : "web", //ensure no browsers are expected
    externals: isProduction
      ? nodeExternals({
          // do not externalize dependencies that need to be processed by webpack.
          // you can add more file types here e.g. raw *.vue files
          // you should also whitelist deps that modifies `global` (e.g. polyfills)
          allowlist: [/\.css$/, /\?vue&type=style/],
        })
      : undefined,
    //   module: isProduction
    //     ? {
    //         rules: [
    //           {
    //             test: /\.vue$/,
    //             loader: "vue-loader",
    //             options: {
    //               // enable CSS extraction
    //               extractCSS: isProduction,
    //             },
    //           },
    //           // ...
    //         ],
    //       }
    //     : undefined,
    plugins: isProduction
      ? // make sure to add the plugin!
        [new ExtractCssChunksPlugin({ filename: "[name].css" })]
      : [],
  },
};
