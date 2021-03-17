import './css/emx2.css'
import App from './App.vue'
import { createApp } from 'vue'
import Icon from './components/ui/icons/Icon.vue'
import routes from './routes.js'
import { createRouter, createWebHistory } from 'vue-router'

const app = {}
app.vm = createApp(App)
app.vm.component('Icon', Icon)

app.vm.directive('click-outside', {
    beforeMount(el, binding) {
        el.clickOutsideEvent = function(event) {
            if (!(el === event.target || el.contains(event.target))) {
                binding.value(event, el)
            }
        }
        document.body.addEventListener('click', el.clickOutsideEvent)
    },
    unmounted(el) {
        document.body.removeEventListener('click', el.clickOutsideEvent)
    },
})

app.router = createRouter({
    history: createWebHistory(),
    linkActiveClass: 'active',
    routes,
})

app.vm.use(app.router).use(app.i18n)
app.vm.mount('#app')