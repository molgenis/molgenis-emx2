export interface IContactInformation {
  title_before_name: string;
  first_name: string;
  last_name: string;
  email: string;
  role: string;
  address: string;
  country: { name: string; label: string };
}

export interface IReportDetail {
  value: string;
  type:
    | "string"
    | "email"
    | "url"
    | "bool"
    | "list"
    | "phone"
    | "report"
    | "string-with-key";
  label?: string;
  badgeColor?: IBadgeColor; // is this actually being used or dead code?
}

interface IBadgeColor {
  type: EBadgeColors;
}

enum EBadgeColors {
  "success",
  "warning",
  "info",
  "secondary",
  "danger",
  "light",
  "dark",
}
