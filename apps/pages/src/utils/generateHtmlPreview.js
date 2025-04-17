export function generateHtmlPreview(instance, content, ref) {
  if (content && typeof content === "object" && Object.keys(content).length) {
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

    if (content.dependencies && Object.keys(content.dependencies).length) {
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
  } else {
    const parser = new DOMParser();
    const doc = parser.parseFromString(content, "text/html");
    /** Loop over the just parsed html items, and add them */
    Array.from(doc.body.children).forEach((el) => {
      if (el.tagName !== "SCRIPT") {
        instance.$refs[ref].appendChild(el);
      } else {
        /** Script tags need a special treatment, else they will not execute. **/
        const scriptEl = document.createElement("script");
        if (el.src) {
          /** If we have an external script. */
          scriptEl.src = el.src;
        } else {
          /** Regular inline script */
          scriptEl.textContent = el.textContent;
        }
        instance.$refs[ref].appendChild(scriptEl);
      }
    });
  }
}
