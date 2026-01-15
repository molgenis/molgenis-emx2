package org.molgenis.emx2;

import java.util.List;

public record GroupPermission(String groupName, List<String> users, List<Permission> permissions) {}
