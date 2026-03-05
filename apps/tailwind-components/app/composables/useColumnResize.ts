import { ref, onBeforeUnmount, type Ref } from "vue";

export function useColumnResize(container: Ref<HTMLElement | null>) {
  const columnWidths = ref<Record<string, number>>({});
  const guideX = ref<number | null>(null);

  const resizingColumn = ref<string | null>(null);

  const startX = ref(0);
  const startWidth = ref(0);

  let rafId: number | null = null;
  let pendingX = 0;

  function setInitialWidths(columns: { id: string }[], defaultWidth = 240) {
    columns.forEach((col) => {
      if (!columnWidths.value[col.id]) {
        columnWidths.value[col.id] = defaultWidth;
      }
    });
  }

  function getRelativeX(clientX: number) {
    const rect = container.value?.getBoundingClientRect();
    return rect ? clientX - rect.left : clientX;
  }

  function startResize(event: MouseEvent, columnId: string) {
    resizingColumn.value = columnId;

    startX.value = event.clientX;
    startWidth.value = columnWidths.value[columnId] ?? 240;

    guideX.value = getRelativeX(event.clientX);

    document.body.style.cursor = "col-resize";

    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", stopResize);
  }

  function handleMouseMove(event: MouseEvent) {
    pendingX = event.clientX;

    if (rafId !== null) return;

    rafId = requestAnimationFrame(updateGuide);
  }

  function updateGuide() {
    guideX.value = getRelativeX(pendingX);
    rafId = null;
  }

  function stopResize() {
    if (!resizingColumn.value) return;

    const diff = pendingX - startX.value;
    const newWidth = startWidth.value + diff;

    const MIN_WIDTH = 80;

    if (newWidth > MIN_WIDTH) {
      columnWidths.value[resizingColumn.value] = newWidth;
    }

    resizingColumn.value = null;
    guideX.value = null;

    document.body.style.cursor = "";

    window.removeEventListener("mousemove", handleMouseMove);
    window.removeEventListener("mouseup", stopResize);

    if (rafId !== null) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }
  }

  onBeforeUnmount(() => stopResize());

  return {
    columnWidths,
    guideX,
    startResize,
    setInitialWidths,
  };
}
