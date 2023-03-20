import { defineStore } from "pinia";
import { Ref, ref } from "vue";
import { useCookies } from "vue3-cookies";
import { request } from "../client/client";
import {
  IErrorMessage,
  IResponse,
  ISession,
} from "../components/account/Interfaces";
import { ISetting } from "../Interfaces/ISetting";

const { cookies } = useCookies();
const defaultSession = {
  locale: "en",
  settings: {} as Record<string, string>,
};
let graphqlError = ""; //TODO figure out how to do error handling

export const useSessionStore = defineStore("session", () => {
  let session: Ref<ISession> = ref(defaultSession);
  updateSession();

  function updateSession() {
    getSession().then((newSession) => {
      session.value = newSession;
    });
  }

  function signin() {
    updateSession();
    console.log("hallo sign in");
  }

  async function signout() {
    const data = await request("graphql", `mutation{signout{status}}`).catch(
      (error: string) => (graphqlError = "internal server error" + error)
    );
    if (data.signout.status === "SUCCESS") {
      updateSession();
    } else {
      graphqlError = "sign out failed";
    }
  }

  return { session, signin, signout };
});

async function getSession(): Promise<ISession> {
  let session: ISession = defaultSession;

  const query = `{
        _session { email, roles, schemas, token, settings{key,value} },
        _settings (keys: ["menu", "page.", "cssURL", "logoURL", "isOidcEnabled","locales"]){ key, value },
        _manifest { ImplementationVersion,SpecificationVersion,DatabaseVersion }
      }`;

  const responses: PromiseSettledResult<IResponse>[] = await Promise.allSettled(
    [request("/apps/central/graphql", query), request("graphql", query)]
  );

  const dbSettings =
    responses[0].status === "fulfilled"
      ? responses[0].value
      : handleError(responses[0].reason);
  const schemaSettings =
    responses[1].status === "fulfilled"
      ? responses[1].value
      : handleError(responses[1].reason);

  if (schemaSettings?._session) {
    session = { ...session, ...schemaSettings._session };
  }

  //convert settings to object
  if (dbSettings?._settings) {
    session.settings = loadSettings(dbSettings, session.settings);
    session.manifest = dbSettings._manifest;
  }

  // schemaSettings override dbSettings if set
  if (schemaSettings?._settings) {
    session.settings = loadSettings(schemaSettings, session.settings);
    session.manifest = schemaSettings._manifest;
  }

  //get locale from cookie
  const lang = cookies.get("MOLGENIS.locale");
  if (lang) {
    session.locale = lang;
  }

  return session;
}

function handleError(reason: IErrorMessage) {}

function loadSettings(
  settings: { _settings: ISetting[] },
  currentSettings: Record<string, string>
): Record<string, string> {
  let newSettings: Record<string, string> = {};
  settings._settings.forEach((setting) => {
    const value: string =
      setting.value?.startsWith("[") || setting.value?.startsWith("{")
        ? parseJson(setting.value)
        : setting.value;
    newSettings[setting.key] = value;
  });
  return { ...currentSettings, ...newSettings };
}

function parseJson(value: string) {
  try {
    return JSON.parse(value);
  } catch (error) {
    // error =
    //   "Parsing of settings failed: " + error + ". value: " + value;
    return null;
  }
}
