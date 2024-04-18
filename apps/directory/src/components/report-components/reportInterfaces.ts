export interface IContactInformation {
  title_before_name: string;
  first_name: string;
  last_name: string;
  email: string;
  role: string;
  address: string;
  country: { name: string; label: string };
}
