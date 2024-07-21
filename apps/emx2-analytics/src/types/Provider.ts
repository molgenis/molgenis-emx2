export type providerId = "site-improve";

export interface Provider {
  id: providerId;
  options: ProviderOptions;
}

export interface ProviderOptions {}

export interface siteImproveOptions extends ProviderOptions {
  analyticsKey: string;
}
