package org.molgenis.emx2;

public interface EmxColumn {
    EmxTable getTable();

    String getName();

    EmxType getType();

    Boolean getNillable();

    Boolean getReadonly();

    String getDefaultValue();

    EmxColumn getRef();

    EmxTable getJoinTable();

    EmxColumn getJoinColumn();

    Boolean getUnique();

    String getValidation();

    String getVisible();

    String getDescription();
}
