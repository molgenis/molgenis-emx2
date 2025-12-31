export function generateHtmlPreview(instance, content, ref) {
  if (content && typeof content === "object" && Object.keys(content).length) {
    instance.$refs[ref].replaceChildren();

    const parser = new DOMParser();
    const documentHead = document.getElementsByTagName("head")[0];

    content.dependencies?.css?.forEach((dependency) => {
      if (dependency.url) {
        const elem = document.createElement("link");
        elem.href = dependency.url;
        elem.rel = "stylesheet";
        documentHead.appendChild(elem);
      }
    });

    content.dependencies?.javascript?.forEach((dependency) => {
      if (dependency.url) {
        const elem = document.createElement("script");
        elem.src = dependency.url;
        if (dependency.defer) {
          elem.defer = true;
        }
        documentHead.appendChild(elem);
      }
    });

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
      scriptElement.text = `setTimeout(() => {
        /** timeout is required for correctly loading external dependencies */
        ${content.javascript}
      }, 200)`;
      instance.$refs[ref].appendChild(scriptElement);
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
