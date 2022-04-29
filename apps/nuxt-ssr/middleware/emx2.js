export default function ({ route, from, store, redirect }) {
    if (route.path === '/') {
        store.state.schema = null;
        redirect('apps/central/');
    } else {
        const schema = route.path.split('/').filter(i => i !== "")[0]
        // console.log('path is:' + route.path);
        // console.log('middleware set schema to: ' + schema);
        store.commit("setSchema", schema);
    }
}
