package org.molgenis.emx2;

import java.util.List;

public record Group(String name, String description, List<Member> members) {}
