export type CssDependency = {
  url: string;
};

export type JavaScriptDependency = {
  url: string;
  defer?: boolean;
};

export type PageBuilderContentMeta = {
  type: "ui" | "editor";
  dateCreated?: string;
  dateModified?: string;
};

export type PageBuilderContent = {
  html: string;
  css: string;
  javascript: string;
  dependencies: {
    css: CssDependency[];
    javascript: JavaScriptDependency[];
  };
  _meta?: PageBuilderContentMeta;
  _settings: Record<string, any>;
};

export function newPageContentObject(
  type: "ui" | "editor"
): PageBuilderContent {
  return {
    css: "",
    html: "",
    javascript: "",
    dependencies: { css: [], javascript: [] },
    _meta: {
      type: type,
    },
    _settings: {},
  };
}

export function newPageDate() {
  const date = new Date().toISOString();
  return date.replace("T", " ").split(".")[0];
}

export function generateHtmlPreview(
  content: PageBuilderContent,
  ref: HTMLDivElement
) {
  if (content && typeof content === "object" && Object.keys(content).length) {
    ref.replaceChildren();

    const parser = new DOMParser();
    const documentHead = document.getElementsByTagName(
      "head"
    )[0] as HTMLHeadElement;

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
        ref.appendChild(element);
      });
    }

    if (content.css) {
      const styleElement = document.createElement("style");
      styleElement.textContent = content.css;
      ref.appendChild(styleElement);
    }

    if (content.javascript) {
      const scriptElement = document.createElement("script");
      scriptElement.setAttribute("type", "text/javascript");
      scriptElement.text = `setTimeout(() => {
        /** timeout is required for correctly loading external dependencies */
        ${content.javascript}
      }, 200)`;
      ref.appendChild(scriptElement);
    }
  } else {
    const parser = new DOMParser();
    const htmlString: string = (content as unknown) as string;
    const doc = parser.parseFromString(htmlString, "text/html");
    /** Loop over the just parsed html items, and add them */
    Array.from(doc.body.children).forEach((el) => {
      if (el.tagName !== "SCRIPT") {
        ref.appendChild(el);
      } else {
        /** Script tags need a special treatment, else they will not execute. **/
        const scriptEl = document.createElement("script");
        if ((el as HTMLScriptElement).src) {
          /** If we have an external script. */
          scriptEl.src = (el as HTMLScriptElement).src;
        } else {
          /** Regular inline script */
          scriptEl.textContent = el.textContent;
        }
        ref.appendChild(scriptEl);
      }
    });
  }
}
