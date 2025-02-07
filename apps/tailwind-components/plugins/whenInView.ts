import { type DirectiveBinding } from 'vue'

type LazyLoadBindingValue = () => Promise<void> | void

export default defineNuxtPlugin((nuxtApp) => {
    //will fire as about half is shown
    nuxtApp.vueApp.directive('when-in-view', {
        mounted(el: HTMLElement, binding: DirectiveBinding<LazyLoadBindingValue>) {
            const options: IntersectionObserverInit = {
                root: null,
                threshold: 0.5,
            };

            const observer = new IntersectionObserver((entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting) {
                        binding.value();
                    }
                });
            }, options);

            observer.observe(el);
        },
    });
});