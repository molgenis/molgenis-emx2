// plugins/lazy-load-directive.client.ts
import { type DirectiveBinding } from 'vue'

type LazyLoadBindingValue = () => Promise<void> | void

export default defineNuxtPlugin((nuxtApp) => {
    nuxtApp.vueApp.directive('on-first-view', {
        mounted(el: HTMLElement, binding: DirectiveBinding<LazyLoadBindingValue>) {
            const options: IntersectionObserverInit = {
                root: null,
                rootMargin: '50px',
                threshold: 0,
            };

            const observer = new IntersectionObserver((entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting) {
                        binding.value();
                        observer.unobserve(el);
                    }
                });
            }, options);

            observer.observe(el);
        },
    });
});