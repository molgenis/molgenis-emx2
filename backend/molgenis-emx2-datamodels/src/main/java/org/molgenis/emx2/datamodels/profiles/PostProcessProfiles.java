package org.molgenis.emx2.datamodels.profiles;

import com.fasterxml.jackson.databind.util.StdConverter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PostProcessProfiles extends StdConverter<Profiles, Profiles> {

  private Pattern delimiter = Pattern.compile(",");

  @Override
  public Profiles convert(Profiles profiles) {
    profiles.datamodelsList = csvStringToList(profiles.datamodels);
    profiles.profilesList = csvStringToList(profiles.profiles);
    return profiles;
  }

  private List<String> csvStringToList(String value) {
    return Optional.ofNullable(value)
        .map(delimiter::splitAsStream)
        .map(x -> x.map(String::trim).collect(Collectors.toList()))
        .orElse(Collections.emptyList());
  }
}
