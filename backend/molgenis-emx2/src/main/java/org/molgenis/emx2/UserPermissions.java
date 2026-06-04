package org.molgenis.emx2;

import java.util.List;
import java.util.Map;

public interface UserPermissions {

  Map<String, TablePermission> getByTable();

  List<TablePermission> getAll();
}
