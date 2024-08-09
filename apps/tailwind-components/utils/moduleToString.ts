export const moduleToString = (module: {
  loc?: { source: { body: string } };
}) => {
  if (module.loc?.source.body === undefined) {
    throw "Unable to load query: " + module.toString();
  }
  return module.loc?.source.body;
};
