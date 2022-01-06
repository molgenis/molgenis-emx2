// const path = require("path");
// const VueLoaderPlugin = require("vue-loader/lib/plugin");
//
// module.exports = {
//   mode: "production",
//   entry: path.resolve("src", "server.js"),
//   output: {
//     filename: "server.js",
//     path: path.join(__dirname, "dist"),
//   },
//   resolve: {
//     alias: {
//       vue: "vue/dist/vue.js",
//     },
//     extensions: [".js", ".json", ".vue"],
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
