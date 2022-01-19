import Vue from 'vue'
import Router from 'vue-router'
import { normalizeURL, decode } from 'ufo'
import { interopDefault } from './utils'
import scrollBehavior from './router.scrollBehavior.js'

const _02e58540 = () => interopDefault(import('../pages/apps/central.vue' /* webpackChunkName: "pages/apps/central" */))
const _8adb470c = () => interopDefault(import('../pages/index.vue' /* webpackChunkName: "pages/index" */))
const _54fbd3eb = () => interopDefault(import('../pages/_schema/index.vue' /* webpackChunkName: "pages/_schema/index" */))
const _1b9ea5f6 = () => interopDefault(import('../pages/_schema/_institutions/index.vue' /* webpackChunkName: "pages/_schema/_institutions/index" */))
const _4d51b905 = () => interopDefault(import('../pages/_schema/_institutions/_institution.vue' /* webpackChunkName: "pages/_schema/_institutions/_institution" */))

const emptyFn = () => {}

Vue.use(Router)

export const routerOptions = {
  mode: 'history',
  base: '/',
  linkActiveClass: 'nuxt-link-active',
  linkExactActiveClass: 'nuxt-link-exact-active',
  scrollBehavior,

  routes: [{
    path: "/apps/central",
    component: _02e58540,
    name: "apps-central"
  }, {
    path: "/",
    component: _8adb470c,
    name: "index"
  }, {
    path: "/:schema",
    component: _54fbd3eb,
    name: "schema"
  }, {
    path: "/:schema/:institutions",
    component: _1b9ea5f6,
    name: "schema-institutions"
  }, {
    path: "/:schema/:institutions/:institution",
    component: _4d51b905,
    name: "schema-institutions-institution"
  }],

  fallback: false
}

export function createRouter (ssrContext, config) {
  const base = (config._app && config._app.basePath) || routerOptions.base
  const router = new Router({ ...routerOptions, base  })

  // TODO: remove in Nuxt 3
  const originalPush = router.push
  router.push = function push (location, onComplete = emptyFn, onAbort) {
    return originalPush.call(this, location, onComplete, onAbort)
  }

  const resolve = router.resolve.bind(router)
  router.resolve = (to, current, append) => {
    if (typeof to === 'string') {
      to = normalizeURL(to)
    }
    return resolve(to, current, append)
  }

  return router
}
