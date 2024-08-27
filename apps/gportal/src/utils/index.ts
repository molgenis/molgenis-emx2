/**
 * cleanURL
 * santise a URL by removing any trailing slashes
 *
 * @param url string containing an URL
 * @returns url as a string
 */
export function cleanUrl(url: string) {
  return url[url.length - 1] === "/" ? url.slice(0, url.length - 1) : url;
}
