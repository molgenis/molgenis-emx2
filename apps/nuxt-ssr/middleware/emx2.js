export default function ({ route, from, store, redirect }) {
    if (route.path === '/') {
        store.state.schema = null
        redirect('apps/central/')
    } else {
        const schema = route.path.split('/').filter(i => i !== "")[0]
        store.commit("setSchema", schema);
    }
}
