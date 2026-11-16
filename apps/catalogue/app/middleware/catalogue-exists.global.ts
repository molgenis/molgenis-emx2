import { createError, defineNuxtRouteMiddleware, useState } from "#app";
import { fetchGql } from "../composables/fetchGql";

export default defineNuxtRouteMiddleware(async (to) => {
  const id = to.params.catalogue;
  if (typeof id !== "string" || id === "all") {
    return;
  }

  const known = useState<Record<string, boolean>>(
    "catalogue-exists",
    () => ({})
  );
  if (known.value[id] === undefined) {
    const resp = await fetchGql<{ Catalogues: { id: string }[] }>(
      "query CatalogueExists($filter:CataloguesFilter){Catalogues(filter:$filter){id}}",
      { filter: { id: { equals: id } } }
    );
    known.value[id] = (resp.data?.Catalogues?.length ?? 0) > 0;
  }

  if (!known.value[id]) {
    throw createError({
      statusCode: 404,
      message: `Catalogue "${id}" doesn't exist`,
      fatal: true,
    });
  }
});
