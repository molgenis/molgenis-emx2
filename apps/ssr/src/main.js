import { createApp } from "./app";

const { app, router } = createApp();

// in server mode the state will be injected statically
// in client mode, for testing, we now also inject below hardcoded
// (todo: provide some UI to select records uit of database)
// state will include {schema: schema, record: record}, with record ingested based on route /schema/view/table/route/key1[/key2]
var state = {
  schema: {
    name: "pet store",
    tables: [{ name: "Pets", columns: [{ id: "name", name: "name" }] }],
  },
  record: { name: "spike" },
};

app.$mount("#app", true);
// });
