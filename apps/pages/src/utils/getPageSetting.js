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
      const pageContentString = pageContent[0].value;
      try {
        return JSON.parse(pageContentString);
      } catch (error) {
        return pageContentString;
      }
    }
  } else {
    return null;
  }
}
