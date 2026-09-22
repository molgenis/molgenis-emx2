import { onMounted, onUnmounted, type Ref } from "vue";

export function useClickOutside(
  elementRef: Ref<HTMLElement | any>,
  callback: () => void
) {
  const handler = (event: MouseEvent) => {
    const el = elementRef.value?.$el || elementRef.value;
    if (!el || typeof el.contains !== "function") {
      return;
    }
    if (!el.contains(event.target as Node)) {
      callback();
    }
  };
  onMounted(() => {
    document.addEventListener("mousedown", handler);
  });

  onUnmounted(() => {
    document.removeEventListener("mousedown", handler);
  });
}
