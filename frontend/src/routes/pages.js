import EditPage from '../views/pages/EditPage.vue'
import ListPages from '../views/pages/ListPages.vue'
import ViewPage from '../views/pages/ViewPage.vue'

export default [
    {
        component: ListPages,
        path: '/',
        props: true,
    },
    {
        component: ViewPage,
        path: '/:page',
        props: true,
    },
    {
        component: EditPage,
        path: '/:page/edit',
        props: true,
    },
]
