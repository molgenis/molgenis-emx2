import EditPage from './components/central/EditPage'
import ListPages from './components/central/ListPages'
import ViewPage from './components/central/ViewPage'

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