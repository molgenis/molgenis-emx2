export function openReAuthenticationWindow(
  parentWindow: Window,
  href: string,
  width = 600,
  height = 400
) {
  const topWindow = parentWindow.top ?? parentWindow;
  const y = topWindow.outerHeight / 2 + topWindow.screenY - height / 2;
  const x = topWindow.outerWidth / 2 + topWindow.screenX - width / 2;

  return parentWindow.open(
    href,
    "_blank",
    `toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=no, resizable=no, copyhistory=no, width=${width}, height=${height}, top=${y}, left=${x}`
  );
}
