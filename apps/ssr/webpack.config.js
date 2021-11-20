// const path = require("path");
// const VueLoaderPlugin = require("vue-loader/lib/plugin");
//
// module.exports = {
//   mode: "production",
//   output: {
//     filename: "[name].js",
//     path: path.join(__dirname, "dist/js"),
//   },
//   resolve: {
//     alias: {
//       vue: "vue/dist/vue.js",
//     },
//     extensions: [".js", ".json", ".vue"],
//     fallback: {
//       fs: false,
//       child_process: false,
//       net: false,
//       tls: false,
//       dns: false,
//     },
//   },
//   module: {
//     rules: [
//       {
//         test: /\.js$/,
//         exclude: /node_modules/,
//         loader: "babel-loader",
//         options: {
//           presets: [
//             [
//               "@babel/env",
//               {
//                 targets: {
//                   ie: 11,
//                 },
//               },
//             ],
//           ],
//         },
//       },
//       {
//         test: /\.vue$/,
//         loader: "vue-loader",
//       },
//     ],
//   },
//   plugins: [new VueLoaderPlugin()],
// };
