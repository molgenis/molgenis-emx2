import type { InjectionKey, Ref } from "vue";

// Popups that teleport (e.g. the date picker menu) must render inside the open
// modal, otherwise the modal's focus trap swallows their clicks.
export const MODAL_TELEPORT_TARGET: InjectionKey<Ref<HTMLElement | null>> =
  Symbol("modalTeleportTarget");

type CloseFn = () => void;

const stack: CloseFn[] = [];

let initialized = false;

function ensureListener() {
  if (initialized || typeof window === "undefined") {
    return;
  }
  initialized = true;

  window.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      // Close ONLY the top-most modal
      stack.at(-1)?.();
    }
  });
}

export function registerModal(close: CloseFn) {
  ensureListener();
  stack.push(close);

  // return cleanup function
  return () => {
    const index = stack.lastIndexOf(close);
    if (index !== -1) {
      stack.splice(index, 1);
    }
  };
}
