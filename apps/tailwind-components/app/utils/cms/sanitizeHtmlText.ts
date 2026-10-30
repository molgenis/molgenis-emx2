import sanitizeHtml from "sanitize-html";

export function sanitizeHtmlText(text: string) {
  return sanitizeHtml(text, {
    transformTags: {
      b: "strong",
      i: "em",
    },
    allowedTags: ["a", "code", "em", "span", "strong", "u", "br"],
    allowedAttributes: {
      a: ["href"],
      "*": ["class"],
    },
  });
}
