export function getPropertyByPath(obj, path) {
  if (!obj) return;
  const pathParts = Array.isArray(path) ? path : path.split(".");

  switch (pathParts.length) {
    case 1:
      return obj[pathParts[0]];
    case 2:
      if (!obj[pathParts[0]]) return;
      return obj[pathParts[0]][pathParts[1]];
    case 3:
      if (!obj[pathParts[0]] || !obj[pathParts[1]]) return;
      return obj[pathParts[0]][pathParts[1]][pathParts[2]];
    case 4:
      if (!obj[pathParts[0]] || !obj[pathParts[1]] || !obj[pathParts[2]])
        return;
      return obj[pathParts[0]][pathParts[1]][pathParts[2]][pathParts[3]];
  }
}
