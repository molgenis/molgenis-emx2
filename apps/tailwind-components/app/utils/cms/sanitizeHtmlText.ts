import sanitizeHtml from "sanitize-html";

export function sanitizeHtmlText(text: string) {
  return sanitizeHtml(text, {
    allowedTags: ["a", "code", "em", "i", "span", "strong"],
  });
}
