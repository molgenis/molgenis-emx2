import React from "react";
import ReactDOM from "react-dom";

import GraphiQL from "graphiql";
import { createGraphiQLFetcher } from "@graphiql/toolkit";

const fetcher = createGraphiQLFetcher({
  url: window.location.origin + "/graphql",
});

ReactDOM.render(
  <GraphiQL fetcher={fetcher} editorTheme={"dracula"} />,
  document.body
);
