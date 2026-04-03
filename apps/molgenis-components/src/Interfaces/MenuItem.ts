export interface MenuItem {
  label: string;
  href: string;
  active?: boolean;
  role?: string;
  submenu: {
    label: string;
    href: string;
    role?: string;
  }[];
}
