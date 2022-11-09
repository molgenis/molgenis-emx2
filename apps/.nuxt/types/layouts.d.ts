import { ComputedRef, Ref } from 'vue'
export type LayoutKey = string
declare module "/Users/umcg-mswertz/git/molgenis-emx2-nuxt3/molgenis-emx2/apps/node_modules/nuxt/dist/pages/runtime/composables" {
  interface PageMeta {
    layout?: false | LayoutKey | Ref<LayoutKey> | ComputedRef<LayoutKey>
  }
}