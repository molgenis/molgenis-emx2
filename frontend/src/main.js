import './css/pyrite.css'
import App from './App.vue'
import { createApp } from 'vue'
import Icon from './components/icons/Icon.vue'

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