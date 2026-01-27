package org.molgenis.emx2.web.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.web.util.SchemaMenu;

class SchemaMenuTest {

  @Test
  void givenViewer_thenHideEditorAndHigherMenuItems() {
    SchemaMenu menu = menu().menuForRole(Privileges.VIEWER.toString());
    List<SchemaMenu.MenuItem> expected =
        List.of(
            new SchemaMenu.MenuItem("n", "n-href", null, "n-key", new ArrayList<>()),
            new SchemaMenu.MenuItem(
                "v", "v-href", Privileges.VIEWER.toString(), "v-key", new ArrayList<>()));
    assertEquals(expected, menu.items());
  }

  @Test
  void givenEditor_thenHideManagerAndHigherMenuItems() {
    SchemaMenu menu = menu().menuForRole(Privileges.EDITOR.toString());
    List<SchemaMenu.MenuItem> expected =
        List.of(
            new SchemaMenu.MenuItem("n", "n-href", null, "n-key", new ArrayList<>()),
            new SchemaMenu.MenuItem(
                "v", "v-href", Privileges.VIEWER.toString(), "v-key", new ArrayList<>()),
            new SchemaMenu.MenuItem(
                "e", "e-href", Privileges.EDITOR.toString(), "e-key", new ArrayList<>()));
    assertEquals(expected, menu.items());
  }

  @Test
  void givenManager_thenShowManagerItems() {
    SchemaMenu menu = menu().menuForRole(Privileges.MANAGER.toString());
    List<SchemaMenu.MenuItem> expected = menu().items();
    assertEquals(expected, menu.items());
  }

  @Test
  void givenAdmin_thenShowAll() {
    SchemaMenu menu = menu().menuForRole("admin");
    List<SchemaMenu.MenuItem> expected = menu().items();
    assertEquals(expected, menu.items());
  }

  private SchemaMenu menu() {
    return SchemaMenu.from(
        new SchemaMenu.MenuItem("n", "n-href", null, "n-key", new ArrayList<>()),
        new SchemaMenu.MenuItem(
            "v", "v-href", Privileges.VIEWER.toString(), "v-key", new ArrayList<>()),
        new SchemaMenu.MenuItem(
            "e", "e-href", Privileges.EDITOR.toString(), "e-key", new ArrayList<>()),
        new SchemaMenu.MenuItem(
            "m", "m-href", Privileges.MANAGER.toString(), "m-key", new ArrayList<>()));
  }
}
