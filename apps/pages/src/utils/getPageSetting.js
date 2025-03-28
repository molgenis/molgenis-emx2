import { request } from "graphql-request";

export async function getPageSetting(pageSettingKey) {
  const query = `query {
    _settings(keys: ["${pageSettingKey}"]) {
      key
      value
    }
  }`;

  const response = await request("graphql", query);

  if (response._settings) {
    const pageContent = response._settings.filter(
      (setting) => setting.key === pageSettingKey
    );
    if (pageContent) {
      const contentString = pageContent[0].value;
      return JSON.parse(contentString);
    }
  } else {
    return null;
  }
}
