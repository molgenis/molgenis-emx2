import { type siteImproveOptions } from "../../types/Provider";
import { type Trigger } from "../../types/Trigger";

declare const _sz: any;

function initialize(options: siteImproveOptions) {
  const scriptSrc = `https://siteimproveanalytics.com/js/siteanalyze_${options.analyticsKey}.js`;
  if (!scriptLoaded(scriptSrc)) {
    // Load the script in scoped script context ( due to cors restrictions)
    const script = document.createElement("script");
    script.async = true;
    script.src = scriptSrc;
    document.body.appendChild(script);
    console.log("site improve script loaded");
  }
}

function handleEvent(event: Event, trigger: Trigger, element: Element) {
  console.log("handel site improve event", event, trigger, element.tagName);

  if (_sz) {
    _sz.push(["event", "demo cat", "demo action", trigger.name]);
  } else {
    console.error("site improve not loaded");
  }
}

function scriptLoaded(scriptSrc: string) {
  const scripts = document.querySelectorAll("script[src]");
  const regex = new RegExp(`^${scriptSrc}`);
  return Boolean(
    Object.values(scripts).filter((value) =>
      regex.test((value as HTMLScriptElement).src)
    ).length
  );
}

export { initialize, handleEvent };
