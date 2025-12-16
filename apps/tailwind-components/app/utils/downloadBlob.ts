export function downloadBlob(
  data: string | undefined,
  mediaType: string,
  fileName: string
) {
  if (!data) return;

  const blob = new Blob([data], { type: mediaType });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement("a");

  a.href = url;
  a.download = fileName;
  a.click();

  window.URL.revokeObjectURL(url);
}
