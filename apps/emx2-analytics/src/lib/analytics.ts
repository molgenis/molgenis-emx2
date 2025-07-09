import { Provider, siteImproveOptions } from "../types/Provider";
import { Trigger } from "../types/Trigger";
import {
  handleEvent as siteImprove,
  initialize as siteImproveInit,
} from "./providers/siteImprove";

function setupAnalytics(schemaName: string, providers: Provider[]) {
  for (let provider of providers) {
    switch (
      provider.id // todo explore fancy dynamic import instead of switch
    ) {
      case "site-improve":
        siteImproveInit(provider.options as siteImproveOptions);
        break;
      default:
        console.error(`Provider ${provider} not supported`);
    }
  }

  fetch(`/${schemaName}/api/trigger`)
    .then((response) => {
      response.json().then((data) => {
        data.forEach((trigger: Trigger) => {
          try {
            const elements = document.querySelectorAll(trigger.cssSelector);
            elements.forEach((element) => {
              console.log(
                `add trigger for ${trigger.name} to ${element.nodeName}`
              );
              element.addEventListener("click", (e) => {
                for (let provider of providers) {
                  handleEvent(e, trigger, element, provider);
                }
              });
            });
          } catch (e) {
            console.error("Failed to select elements for trigger", trigger);
            console.error(`Error: ${e} for ${trigger.name}`);
          }
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
  provider: Provider
) {
  switch (provider.id) {
    case "site-improve":
      siteImprove(event, trigger, element);
      break;
    default:
      console.error(`Provider ${provider.id} not supported`);
  }
}

export { setupAnalytics };
