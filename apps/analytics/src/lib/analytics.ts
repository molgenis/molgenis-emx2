import { provider, Trigger } from "../types/Trigger";
import { handleEvent as siteImprove } from "./providers/siteImprove";

function setupAnalytics(schemaName: string, providers: provider[]) {
  fetch(`/${schemaName}/api/trigger`)
    .then((response) => {
      response.json().then((data) => {
        data.forEach((trigger: Trigger) => {
          const elements = document.querySelectorAll(trigger.cssSelector);
          elements.forEach((element) => {
            console.log(`Setting up trigger for ${trigger.name}`);
            element.addEventListener("click", (e) => {
              for (let provider of providers) {
                handleEvent(e, trigger, element, provider);
              }
            });
          });
        });
      });
    })
    .catch((error) => {
      console.error("Error:", error);
    });
}

function handleEvent(
  event: Event,
  trigger: Trigger,
  element: Element,
  provider: provider
) {
  switch (provider) {
    case "site-improve":
      siteImprove(event, trigger, element);
      break;
    default:
      console.error(`Provider ${provider} not supported`);
  }
}

export { setupAnalytics };
