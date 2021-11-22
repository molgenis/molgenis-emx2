import { createApp } from "./app";

const { app, router } = createApp();

// in server mode the state will be injected statically
// in client mode, for testing, we now also inject below hardcoded
// (todo: provide some UI to select records uit of database)
// state will include {schema: schema, record: record}, with record ingested based on route /schema/view/table/route/key1[/key2]

//retrieve state based on url
app.$mount("#app", true);
// });
