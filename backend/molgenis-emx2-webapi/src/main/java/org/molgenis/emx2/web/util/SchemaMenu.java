package org.molgenis.emx2.web.util;

import static org.molgenis.emx2.Privileges.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;

public record SchemaMenu(List<MenuItem> items) {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static SchemaMenu from(MenuItem... items) {
    return new SchemaMenu(List.of(items));
  }

  public static SchemaMenu fromSchema(Schema schema) {
    Optional<String> menuSettingValue = schema.getMetadata().findSettingValue("menu");
    if (menuSettingValue.isEmpty()) {
      return new SchemaMenu(new ArrayList<>());
    }

    try {
      List<MenuItem> items =
          MAPPER.readerForListOf(MenuItem.class).readValue(menuSettingValue.get());
      return new SchemaMenu(items);
    } catch (JsonProcessingException e) {
      throw new MolgenisException("Invalid json provided for menu", e);
    }
  }

  public SchemaMenu menuForRole(String role) {
    return new SchemaMenu(items.stream().filter(item -> shouldShow(item, role)).toList());
  }

  @Override
  public List<MenuItem> items() {
    return new ArrayList<>(items);
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }

  private boolean shouldShow(MenuItem item, String role) {
    return role == null
        || role.equals("admin")
        || item.role() == null
        || (item.role().equals(VIEWER.toString())
            && List.of(VIEWER.toString(), EDITOR.toString(), MANAGER.toString()).contains(role))
        || (item.role().equals(EDITOR.toString())
            && List.of(EDITOR.toString(), MANAGER.toString()).contains(role))
        || (item.role().equals(MANAGER.toString()) && role.equals(MANAGER.toString()));
  }

  public record MenuItem(
      String label, String href, String role, String key, List<MenuItem> submenu) {}
}
