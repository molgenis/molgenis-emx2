import type { NavigationGuard } from 'vue-router'
export type MiddlewareKey = string
declare module "/Users/connor/Code/emx2/molgenis-emx2/apps/nuxt3-ssr/node_modules/nuxt/dist/pages/runtime/composables" {
  interface PageMeta {
    middleware?: MiddlewareKey | NavigationGuard | Array<MiddlewareKey | NavigationGuard>
  }
}