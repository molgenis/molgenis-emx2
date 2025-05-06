package org.molgenis.emx2.yaml;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;

public class Entity {
  @JsonProperty(required = true)
  private String entity;

  private String label;
  private Map<String, String> labels;
  private String description;
  private Map<String, String> descriptions;
  private List<String> notes;
  private List<Variant> variants;

  @JsonProperty(required = true)
  private List<Field> fields;

  @JsonProperty("import")
  private String import_path;

  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> import_fields;

  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> import_variants;

  @JsonIgnore private URL sourceURL;

  // methods

  public String getEntity() {
    return entity;
  }

  public void setEntity(String entity) {
    this.entity = entity;
  }

  public String getImport_path() {
    return import_path;
  }

  public void setImport_path(String import_path) {
    this.import_path = import_path;
  }

  public List<Field> getFields() {
    return fields;
  }

  public void setFields(List<Field> fields) {
    this.fields = fields;
  }

  public URL getSourceURL() {
    return sourceURL;
  }

  public void setSourceURL(URL baseUrl) {
    this.sourceURL = baseUrl;
    if (fields != null) {
      fields.forEach(field -> field.setSourceURL(baseUrl));
    }
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Map<String, String> getDescriptions() {
    return descriptions;
  }

  public void setDescriptions(Map<String, String> descriptions) {
    this.descriptions = descriptions;
  }

  public List<String> getNotes() {
    return notes;
  }

  public void setNotes(List<String> notes) {
    this.notes = notes;
  }

  public List<Variant> getVariants() {
    return variants;
  }

  public void setVariants(List<Variant> variants) {
    this.variants = variants;
  }

  public List<String> getImport_fields() {
    return import_fields;
  }

  public void setImport_fields(List<String> import_fields) {
    this.import_fields = import_fields;
  }

  public List<String> getImport_variants() {
    return import_variants;
  }

  public void setImport_variants(List<String> import_variants) {
    this.import_variants = import_variants;
  }

  public void loadImports() {
    if (fields != null) {
      List<Field> result = new ArrayList<>();
      for (Field field : fields) {
        if (field.getImport_path() == null) {
          // check if it is overriding an imported field
          Map<String, Field> currentFields =
              result.stream()
                  .filter(f -> f.getField() != null)
                  .collect(Collectors.toMap(Field::getField, f -> f));
          if (currentFields.containsKey(field.getField())) {
            currentFields.get(field.getField()).overrideProperties(field);
          } else {
            // not an import, use field definition
            result.add(field);
          }
        } else {
          // import the field(s)
          try {
            // try to be an entity
            Entity importedEntity =
                Yaml2Loader.loadEntity(
                    Yaml2Loader.resolveImportUrl(
                        field.getImport_path(), Yaml2Loader.getBaseUrl(sourceURL)));
            importedEntity.applyFieldsFilter(field.getImport_fields());
            result.addAll(importedEntity.fields);
            field.setImport_fields(null);
          } catch (Exception e) {
            // might be one field
            result.add(
                Yaml2Loader.loadField(
                    Yaml2Loader.resolveImportUrl(
                        field.getImport_path(), Yaml2Loader.getBaseUrl(sourceURL))));
            // don't catch exception ... will be real error
          }
        }
      }
      this.fields = result;
    }
  }

  public void applyFieldsFilter(List<String> filterFields) {
    if (filterFields != null) {
      this.setFields(
          filterFields.stream()
              .map(
                  fieldName -> {
                    // find the field or throw error
                    Optional<Field> theField =
                        this.getFields().stream()
                            .filter(field -> fieldName.equals(field.getField()))
                            .findFirst();
                    if (theField.isPresent()) {
                      return theField.get();
                    } else {
                      throw new MolgenisException(
                          "import filter failed: field '" + fieldName + "' not found");
                    }
                  })
              .toList());
    }
  }

  public void applyVariantsFilter(List<String> filterVariants) {
    if (variants != null && filterVariants != null) {

      // get all extended variants
      Map<String, Variant> variantMap =
          variants.stream().collect(Collectors.toMap(Variant::getVariant, Function.identity()));
      Set<String> filterVariantsExtended = new HashSet<>();
      for (String name : filterVariants) {
        collectExtends(name, variantMap, filterVariantsExtended);
      }

      // apply to variants
      this.setVariants(
          variants.stream()
              .filter(variant -> filterVariantsExtended.contains(variant.getVariant()))
              .toList());

      // apply to fields
      if (fields != null && filterVariants != null) {
        // then filter all fields
        this.setFields(
            fields.stream()
                .filter(
                    field ->
                        field.getVariant() == null
                            || filterVariantsExtended.contains(field.getVariant()))
                .toList());
      }
    }
  }

  private static void collectExtends(String name, Map<String, Variant> map, Set<String> acc) {
    if (name == null || acc.contains(name)) return;
    acc.add(name);
    Variant v = map.get(name);
    if (v != null && v.getExtendsVariants() != null) {
      for (String parent : v.getExtendsVariants()) {
        collectExtends(parent, map, acc);
      }
    }
  }

  public void overrideProperties(Entity otherEntity) {
    if (otherEntity.entity != null) this.entity = otherEntity.entity;
    if (otherEntity.label != null) this.label = otherEntity.label;
    if (otherEntity.description != null) this.description = otherEntity.description;
    if (otherEntity.notes != null) this.notes = new ArrayList<>(otherEntity.notes);
    if (otherEntity.labels != null) {
      if (this.labels == null) this.labels = new LinkedHashMap<>();
      this.labels.putAll(otherEntity.labels);
    }
    if (otherEntity.descriptions != null) {
      if (this.descriptions == null) this.descriptions = new LinkedHashMap<>();
      this.descriptions.putAll(otherEntity.descriptions);
    }

    if (otherEntity.variants != null && otherEntity.variants.size() > 0) {
      if (this.variants == null) this.variants = new ArrayList<>();
      else this.variants = new ArrayList<>(this.variants); // otherwise immutable
      Map<String, Variant> overrideVariants =
          otherEntity.variants.stream()
              .collect(Collectors.toMap(Variant::getVariant, variant -> variant));
      Map<String, Variant> myVariants =
          this.variants.stream().collect(Collectors.toMap(Variant::getVariant, variant -> variant));
      for (Variant overrideVariant : overrideVariants.values()) {
        if (myVariants.containsKey(overrideVariant.getVariant())) {
          myVariants.get(overrideVariant.getVariant()).overrideProperties(overrideVariant);
        } else {
          // added a field not in the import
          this.variants.add(overrideVariant);
        }
      }
    }

    if (otherEntity.fields != null && otherEntity.fields.size() > 0) {
      if (this.fields == null) this.fields = new ArrayList<>();
      else this.fields = new ArrayList<>(this.fields); // otherwise immutable
      Map<String, Field> overrideFields =
          otherEntity.fields.stream().collect(Collectors.toMap(Field::getField, field -> field));
      Map<String, Field> myFields =
          this.fields.stream().collect(Collectors.toMap(Field::getField, field -> field));
      for (Field overrideField : overrideFields.values()) {
        if (myFields.containsKey(overrideField.getField())) {
          myFields.get(overrideField.getField()).overrideProperties(overrideField);
        } else {
          // added a field not in the import
          this.fields.add(overrideField);
        }
      }
    }
  }
}
