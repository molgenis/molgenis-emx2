import { defineContentConfig, defineCollection, z } from "@nuxt/content";

export default defineContentConfig({
  collections: {
    docs: defineCollection({
      type: "page",
      source: "**/*.md",
      schema: z.object({
        title: z.string().optional(),
        description: z.string().optional(),
      }),
    }),
  },
});
