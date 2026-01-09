import { onMounted, onUnmounted, type Ref } from 'vue';

export function useClickOutside(
    elementRef: Ref<HTMLElement | any>,
    callback: () => void
) {
  const handler = (event: MouseEvent) => {
    // Get the actual DOM element
    // If it's a Vue component instance, use $el property
    // Otherwise use the ref value directly
    const el = elementRef.value?.$el || elementRef.value;

    // Only proceed if we have a valid element with contains method
    if (!el || typeof el.contains !== 'function') {
      return;
    }

    // Check if click is outside the element
    if (!el.contains(event.target as Node)) {
      callback();
    }
  };

  onMounted(() => {
    // Use mousedown instead of click for better UX
    // (prevents issues with select/input focus)
    document.addEventListener('mousedown', handler);
  });

  onUnmounted(() => {
    document.removeEventListener('mousedown', handler);
  });
}
