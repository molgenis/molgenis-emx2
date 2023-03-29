package org.molgenis.emx2.io.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.molgenis.emx2.MolgenisException;

public class ModelRepository {
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    Map<String, YamlTable> archetypes = new LinkedHashMap<>();
    Map<String, YamlSchema> standards = new LinkedHashMap<>();
    Map<String, YamlSchema> instances = new LinkedHashMap<>();

    public ModelRepository(File archetypesDir, File instancesDir) {

        Arrays.stream(archetypesDir.listFiles())
                .forEach(
                        archetypeFile -> {
                            archetypes.put(
                                    archetypeFile.getName().replace(".yaml", ""), readYamlTable(archetypeFile));
                        });
        Arrays.stream(instancesDir.listFiles())
                .forEach(
                        instanceFile -> {
                            instances.put(
                                    instanceFile.getName().replace(".yaml", ""), readYamlSchema(instanceFile));
                        });
    }

    public static YamlTable readYamlTable(File file) {
        try {
            YamlSchemaValidator.validate(mapper.readTree(file));
            return mapper.readValue(file, YamlTable.class);
        } catch (IOException e) {
            throw new MolgenisException("Parsing " + file.getName() + " failed: \n" + e.getMessage());
        }
    }

    public static YamlSchema readYamlSchema(File file) {
        try {
            YamlSchemaValidator.validate(mapper.readTree(file));
            return mapper.readValue(file, YamlSchema.class);
        } catch (IOException e) {
            throw new MolgenisException("Parsing " + file.getName() + " failed: \n" + e.getMessage());
        }
    }

    public Map<String, YamlTable> getArchetypes() {
        return archetypes;
    }

    public void setArchetypes(Map<String, YamlTable> archetypes) {
        this.archetypes = archetypes;
    }

    public Map<String, YamlSchema> getStandards() {
        return standards;
    }

    public void setStandards(Map<String, YamlSchema> standards) {
        this.standards = standards;
    }

    public Map<String, YamlSchema> getInstances() {
        return instances;
    }

    public void setInstances(Map<String, YamlSchema> instances) {
        this.instances = instances;
    }
}
