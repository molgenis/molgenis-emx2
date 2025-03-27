export function generateHtmlPreview(instance, content, ref) {
  if (content) {
    instance.$refs[ref].replaceChildren();

    const parser = new DOMParser();

    if (content.html) {
      const doc = parser.parseFromString(content.html, "text/html");
      Array.from(doc.body.children).forEach((element) => {
        instance.$refs[ref].appendChild(element);
      });
    }

    if (content.css) {
      const styleElement = document.createElement("style");
      styleElement.textContent = content.css;
      instance.$refs[ref].appendChild(styleElement);
    }

    if (content.javascript) {
      const scriptElement = document.createElement("script");
      scriptElement.setAttribute("type", "text/javascript");
      scriptElement.text = content.javascript;
      instance.$refs[ref].appendChild(scriptElement);
    }

    if (content.dependencies) {
      if (content.dependencies.css.length) {
        content.dependencies.css.forEach((url) => {
          if (url && url !== "") {
            const elem = document.createElement("link");
            elem.href = url;
            elem.rel = "stylesheet";
            instance.$refs[ref].appendChild(elem);
          }
        });
      }

      if (content.dependencies.javascript.length) {
        content.dependencies.javascript.forEach((url) => {
          if (url && url !== "") {
            const elem = document.createElement("link");
            elem.src = url;
            elem.defer = true;
            instance.$refs[ref].appendChild(elem);
          }
        });
      }
    }
  }
}
