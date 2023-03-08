import { createStore, createLogger } from "vuex";

export default createStore({
  strict: true,
  plugins: [createLogger()],
});
