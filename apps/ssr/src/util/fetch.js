// /* Create fetch ponyfill, because that is not native in node. For playing around with server side graphql fetch */
// //https://community.tealiumiq.com/t5/TLC-Blog/Tealium-Functions/ba-p/36392
// export default (...args) => {
//   if (args.length === 0) {
//     throw new TypeError(
//       "Failed to execute 'fetch': 1 argument required, but only 0 present."
//     );
//   }
//
//   const request =
//     args[0] instanceof Request
//       ? new Request(args[0].url, Object.assign({}, args[0], args[1]))
//       : new Request(args[0], args[1]);
//
//   return new Promise((resolve, reject) => {
//     httpClient.send(
//       {
//         uri: request.url,
//         body: request.body,
//         method: request.method,
//         headers: request.headers.map,
//       },
//       (response) => {
//         if (response.statusCode) {
//           return resolve(
//             new Response(response.body, {
//               status: response.statusCode,
//               headers: response.headers,
//               body: response.body,
//               url: request.url,
//             })
//           );
//         } else {
//           return reject(new TypeError(response.errorMessage));
//         }
//       }
//     );
//   });
// };
