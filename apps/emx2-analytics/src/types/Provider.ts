export const PROVIDER_IDS = ["site-improve"] as const;

export type providerId = (typeof PROVIDER_IDS)[number];
export interface Provider {
  id: providerId;
  options: ProviderOptions;
}

export interface ProviderOptions {}

export interface siteImproveOptions extends ProviderOptions {
  analyticsKey: string;
}

export function isProviderId(value: string): value is providerId {
  return PROVIDER_IDS.includes(value as providerId);
}
