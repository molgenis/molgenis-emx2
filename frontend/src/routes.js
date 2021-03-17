import EditPage from './components/pages/EditPage.vue'
import ListPages from './components/pages/ListPages.vue'
import ViewPage from './components/pages/ViewPage.vue'

export default [
    // Pages
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