import { Trigger } from "../../types/Trigger";

function handleEvent(event: Event, trigger: Trigger, element: Element) {
  console.log("handel site improve event");
  console.log("Event triggered", event, trigger, element.tagName);
}

export { handleEvent };
