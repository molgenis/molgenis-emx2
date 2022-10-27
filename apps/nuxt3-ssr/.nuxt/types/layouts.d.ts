import { ComputedRef, Ref } from 'vue'
export type LayoutKey = string
declare module "/Users/connor/Code/emx2/molgenis-emx2/apps/nuxt3-ssr/node_modules/nuxt/dist/pages/runtime/composables" {
  interface PageMeta {
    layout?: false | LayoutKey | Ref<LayoutKey> | ComputedRef<LayoutKey>
  }
}