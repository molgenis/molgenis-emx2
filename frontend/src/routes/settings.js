import Layout from '@/components/settings/Layout.vue'
import Members from '@/components/settings/Members.vue'
import MenuManager from '@/components/settings/MenuManager.vue'
import PageManager from '@/components/settings/PageManager.vue'

export default [
  {
    component: Layout,
    name: 'Layout',
    path: '/Layout',
  },
  {
    component: Members,
    name: 'Members',
    path: '/Members',
  },
  {
    component: MenuManager,
    name: 'Menu',
    path: '/Menu',
  },
  {
    component: PageManager,
    name: 'Pages',
    path: '/Pages',
  },
  {
    path: '/',
    redirect: '/members',
  },
]

