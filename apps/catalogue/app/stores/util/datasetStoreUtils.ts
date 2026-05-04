import type { IResources } from "../../../interfaces/catalogue";

export async function handleV3Error(response: Response) {
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

export function toNegotiatorFormat(datasets: Record<string, IResources>) {
  return Object.values(datasets).map((dataset) => ({
    id: dataset.pid,
    name: dataset.name,
  }));
}

export function getHumanReadableString(datasets: Record<string, IResources>) {
  const datasetInfo = Object.values(datasets).map((dataset) => {
    return { pid: dataset.pid, name: dataset.name };
  });
  const humanReadableString = datasetInfo
    .reduce((acc, dataset) => {
      return acc + `${dataset.name} (${dataset.pid}), `;
    }, "")
    .slice(0, -2);

  return humanReadableString;
}
