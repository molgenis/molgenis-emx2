import ListTables from '@/components/tables/ListTables.vue'
import ViewTable from '@/components/tables/ViewTable.vue'

export default [
  {
    component: ListTables,
    path: '/',
    props: true,
  },
  {
    component: ViewTable,
    path: '/:table',
    props: true,
  },
]
