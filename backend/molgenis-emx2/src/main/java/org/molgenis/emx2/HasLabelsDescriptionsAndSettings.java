package org.molgenis.emx2;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class HasLabelsDescriptionsAndSettings<T> extends HasSettings<T> {
  // long names, with locale as keyl, label as value
  // options
  protected Map<String, String> labels = new TreeMap<>();
  // long description of the column, locale as key, description as value
  protected Map<String, String> descriptions = new TreeMap<>();

  public T setLabels(Map<String, String> newLabels) {
    Objects.requireNonNull(newLabels);
    this.labels = new TreeMap<>();
    // strip empty strings
    newLabels.entrySet().stream()
        .forEach(
            entry -> {
              if (entry.getValue() != null && !"".equals(entry.getValue().trim())) {
                this.labels.put(entry.getKey(), entry.getValue());
              }
            });
    return (T) this;
  }

  public T setLabel(String label) {
    this.setLabel(label, "en"); // 'en' is the default
    return (T) this;
  }

  public T setLabel(String label, String locale) {
    Objects.requireNonNull(locale);
    if (label == null || label.trim().equals("")) {
      this.labels.remove(locale);
    } else {
      this.labels.put(locale, label);
    }
    return (T) this;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public Map<String, String> getDescriptions() {
    return descriptions;
  }

  public T setDescriptions(Map<String, String> newDescriptions) {
    // strip empty strings
    newDescriptions.entrySet().stream()
        .forEach(
            entry -> {
              this.descriptions.put(entry.getKey(), entry.getValue());
            });
    return (T) this;
  }

  public T setDescription(String newDescription) {
    this.setDescription(newDescription, "en"); // 'en' is the default
    return (T) this;
  }

  public T setDescription(String description, String locale) {
    Objects.requireNonNull(locale);
    this.descriptions.put(locale, description);
    return (T) this;
  }
}
