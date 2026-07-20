import type { ICartItem } from "~~/interfaces/types";

export async function doNegotiatorV3Request(
  cartItems: ICartItem[],
  storeUrl: string
) {
  const url = window.location.origin;
  const humanReadable = toHumanReadableString(cartItems);
  const resources = toNegotiatorFormat(cartItems);
  const payload: Record<string, any> = { url, humanReadable, resources };

  return await fetch(storeUrl, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
}

export async function handleNegotiatorV3Error(response: Response) {
  const statusCode = response.status;
  const jsonResponse = await response.json();
  const detail = jsonResponse.detail ? ` Detail: ${jsonResponse.detail}` : "";
  switch (statusCode) {
    case 400:
      const error400 = `Negotiator responded with code 400, invalid input.${detail}`;
      console.error(error400);
      return error400;
    case 401:
      const error401 = `Negotiator responded with code 401, not authorised.${detail}`;
      console.error(error401);
      return error401;
    case 404:
      const error404 = `Negotiator not found, error code 404.${detail}`;
      console.error(error404);
      return error404;
    case 413:
      const error413 = `Negotiator responded with code 413, request too large.${detail}`;
      console.error(error413);
      return error413;
    case 500:
      const error500 = `Negotiator responded with code 500, internal server error.${detail}`;
      console.error(error500);
      return error500;
    default:
      const errorUnknown = `An unknown error occurred with the Negotiator. Please try again later.${detail}`;
      console.error(errorUnknown);
      return errorUnknown;
  }
}

function toNegotiatorFormat(cartItems: ICartItem[]) {
  return cartItems
    .filter(
      (i): i is Extract<ICartItem, { type: "resource" }> =>
        i.type === "resource"
    )
    .map((item) => {
      return { id: item.data.pid, name: item.data.name };
    });
}

function toHumanReadableString(cartItems: ICartItem[]) {
  const resources = cartItems.filter(
    (i): i is Extract<ICartItem, { type: "resource" }> => i.type === "resource"
  );
  const variables = cartItems.filter(
    (i): i is Extract<ICartItem, { type: "variable" }> => i.type === "variable"
  );
  const parts = [];
  if (resources.length > 0) {
    parts.push(
      "Resources: " +
        resources
          .map((resource) => `${resource.data.name} (${resource.data.pid})`)
          .join(", ")
    );
  }
  if (variables.length > 0) {
    parts.push(
      "Variables: " + variables.map((variable) => variable.label).join(", ")
    );
  }
  return parts.join("; ");
}
