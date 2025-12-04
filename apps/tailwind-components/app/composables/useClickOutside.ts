import { onMounted, onBeforeUnmount, type Ref } from "vue";

export function useClickOutside(
  elRef: Ref<HTMLElement | null>,
  callback: () => void
) {
  function handler(event: MouseEvent) {
    if (!elRef.value) return;
    if (!(event.target instanceof Node)) return;
    if (!elRef.value.contains(event.target)) callback();
  }

  onMounted(() => document.addEventListener("click", handler));
  onBeforeUnmount(() => document.removeEventListener("click", handler));
}
