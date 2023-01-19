import("graphql-playground-react/build/static/css/index.css");
await import("graphql-playground-react/build/static/js/middleware.js");

window.GraphQLPlayground.init(document.getElementById("root"), {
  endpoint: "./graphql",
  settings: {
    "schema.polling.enable": false,
    "request.credentials": "include",
  },
  // options as 'endpoint' belong here
});
