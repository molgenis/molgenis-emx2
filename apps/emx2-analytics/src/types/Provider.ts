export type providerId = "site-improve" | "google-analytics" | "piwik-pro";
export interface Provider {
  id: providerId;
  options: ProviderOptions;
}

export interface ProviderOptions {}

export interface siteImproveOptions extends ProviderOptions {
  analyticsKey: string;
}
