export function dispatchKeyPress(
  element: Element,
  keyCode: number
): KeyboardEvent {
  const event = new KeyboardEvent("keypress", {
    key: String.fromCharCode(keyCode),
    charCode: keyCode,
    keyCode,
    which: keyCode,
    bubbles: true,
    cancelable: true,
  });
  element.dispatchEvent(event);
  return event;
}
