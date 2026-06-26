import { defineNuxtModule, installModule } from "nuxt/kit";

export default defineNuxtModule({
  meta: { name: "content" },
  async setup() {
    await installModule("@nuxt/content");
  },
});
