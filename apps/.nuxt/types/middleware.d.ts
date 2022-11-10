import type { NavigationGuard } from 'vue-router'
export type MiddlewareKey = string
declare module "/Users/umcg-mswertz/git/molgenis-emx2-nuxt3/molgenis-emx2/apps/node_modules/nuxt/dist/pages/runtime/composables" {
  interface PageMeta {
    middleware?: MiddlewareKey | NavigationGuard | Array<MiddlewareKey | NavigationGuard>
  }
}