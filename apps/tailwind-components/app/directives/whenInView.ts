import type { DirectiveBinding, ObjectDirective } from "vue";

type InViewHandler = () => Promise<void> | void;

/** `null` switches the directive off, so a caller skips the observer without a second template branch. */
export type InViewBinding =
  | InViewHandler
  | [InViewHandler, IntersectionObserverInit]
  | null
  | false;

//will fire as about half is shown, unless the caller passes its own options
const DEFAULT_OPTIONS: IntersectionObserverInit = {
  root: null,
  threshold: 0.5,
};

const observers = new WeakMap<HTMLElement, IntersectionObserver>();

/** A Nuxt plugin registers this globally; a test registers the same object through `global.directives`. */
export const whenInView: ObjectDirective<HTMLElement, InViewBinding> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<InViewBinding>) {
    if (!binding.value) {
      return;
    }
    const [handler, options] = Array.isArray(binding.value)
      ? binding.value
      : [binding.value, DEFAULT_OPTIONS];

    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          handler();
        }
      });
    }, options);

    observer.observe(el);
    observers.set(el, observer);
  },
  unmounted(el: HTMLElement) {
    observers.get(el)?.disconnect();
    observers.delete(el);
  },
};
