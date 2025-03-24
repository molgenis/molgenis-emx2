import { type DirectiveBinding } from "vue";
type LazyLoadBindingValue = () => Promise<void> | void;

//this directive will fire when the top of an element scrolls to/over the top of a scrollable container
export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.directive("when-overlaps-with-top-of-container", {
    mounted(el: HTMLElement, binding: DirectiveBinding<LazyLoadBindingValue>) {
      const container = findScrollableContainer(el);
      if (!container) {
        console.warn(
          "No scrollable container found for when-overlaps-with-top-of-container directive."
        );
        return;
      }

      const dynamicThreshold = calculateDynamicThreshold(el, container);

      const handleOverlapping = () => {
        const containerRect = container.getBoundingClientRect();
        const elRect = el.getBoundingClientRect();
        if (
          elRect.top <= containerRect.top + dynamicThreshold + 1 &&
          elRect.bottom > containerRect.top
        ) {
          binding.value();
        }
      };

      // Initial check on mount
      handleOverlapping();

      // Re-check on scroll
      container.addEventListener("scroll", handleOverlapping);

      // (Optional) Re-check on resize (if the layout might change significantly)
      window.addEventListener("resize", handleOverlapping);

      // Store it for removal
      (el as any).handleOverlapping = handleOverlapping;
    },
    unmounted(el: HTMLElement) {
      // Clean up event listeners
      const container = findScrollableContainer(el);
      if (container) {
        container.removeEventListener("scroll", (el as any).handleOverlapping);
      }
      window.removeEventListener("resize", (el as any).handleOverlapping);
    },
  });
});

function isScrollable(element: HTMLElement): boolean {
  const style = getComputedStyle(element);
  const overflowY = style.overflowY;
  return overflowY === "auto" || overflowY === "scroll";
}

function findScrollableContainer(element: HTMLElement): HTMLElement | null {
  let currentElement = element.parentElement as HTMLElement;
  while (currentElement) {
    if (isScrollable(currentElement)) {
      // Check for scrollability
      return currentElement;
    }
    currentElement = currentElement.parentElement as HTMLElement;
  }
  return null;
}

function calculateDynamicThreshold(
  element: HTMLElement,
  container: HTMLElement
): number {
  let threshold = 0;
  let currentElement: HTMLElement | null = element;

  while (currentElement && currentElement !== container) {
    const style = getComputedStyle(currentElement);
    threshold += parseFloat(style.marginTop) || 0;
    threshold += parseFloat(style.paddingTop) || 0;
    currentElement = currentElement.parentElement as HTMLElement;
  }

  return threshold;
}
