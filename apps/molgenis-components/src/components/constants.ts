export default {
  CODE_0: 48,
  CODE_9: 57,
  CODE_PERIOD: 46,
  CODE_MINUS: 45,
  SCHEMA_NAME_REGEX: /^(?!.* _|.*_ )[a-zA-Z][-a-zA-Z0-9 _]{0,62}$/,
  TABLE_NAME_REGEX: /^(?!.* _|.*_ )[a-zA-Z][a-zA-Z0-9 _]{0,30}$/,
  COLUMN_NAME_REGEX: /^(?!.* _|.*_ )[a-zA-Z][a-zA-Z0-9 _]{0,62}$/,
  EMAIL_REGEX:
    /^(([^<>()\\[\]\\.,;:\s@"]+(\.[^<>()\\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$|^$/,
  HYPERLINK_REGEX:
    /^((https?):\/\/)(www.)?[-a-zA-Z0-9@:%._\\+~#?&//=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%._\\+~#?&//=()]*)\/?$|^$/,
  PERIOD_REGEX: /^P(?=\d+Y|\d+M|\d+D)(\d+Y)?(\d+M)?(\d+D)?$/,
  MAX_LONG: "9223372036854775807",
  MIN_LONG: "-9223372036854775807",
  IS_CHAPTERS_ENABLED_FIELD_NAME: "isChaptersEnabled",
  AUTO_ID: "AUTO_ID",
  HEADING: "HEADING",
};

const LEVEL_1 = "Level 1";
const LEVEL_2 = "Level 2";
const LEVEL_3 = "Level 3";
const LEVEL_4 = "Level 4";
const CUSTOM = "Custom";
const POLICY_LEVEL_KEY = "PrivacyPolicyLevel";
const POLICY_TEXT_KEY = "PrivacyPolicyText";
const PREFABS = {
  [LEVEL_1]: "No privacy data",
  [LEVEL_2]: "Pseudomized data",
  [LEVEL_3]: "Privacy data",
  [LEVEL_4]: "Privacy data + medical",
  [CUSTOM]: "",
};

export const privacyConstants = {
  LEVEL_1,
  LEVEL_2,
  LEVEL_3,
  LEVEL_4,
  CUSTOM,
  POLICY_LEVEL_KEY,
  POLICY_TEXT_KEY,
  PREFABS,
};
