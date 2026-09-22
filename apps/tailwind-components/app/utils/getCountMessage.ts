export function getCountMessage(
  page: number,
  pageSize: number,
  totalCount: number
) {
  if (totalCount === 0) {
    return "";
  }

  const start = (page - 1) * pageSize + 1;
  const end = Math.min(page * pageSize, totalCount);
  return `Showing ${start} to ${end} of ${totalCount} items`;
}
