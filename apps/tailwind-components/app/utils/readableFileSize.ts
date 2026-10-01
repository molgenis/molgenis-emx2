export function readableFileSize(size: number) {
  if (isNaN(size)) return "Unknown";
  if (size < 1024) return size + " Bytes";
  if (size < 1024 * 1024) return (size / 1024).toFixed(0) + " KB";
  if (size < 1024 * 1024 * 1024)
    return (size / (1024 * 1024)).toFixed(0) + " MB";
  if (size < 1024 * 1024 * 1024 * 1024)
    return (size / (1024 * 1024 * 1024)).toFixed(2) + " GB";
  return (size / (1024 * 1024 * 1024 * 1024)).toFixed(2) + " TB";
}
