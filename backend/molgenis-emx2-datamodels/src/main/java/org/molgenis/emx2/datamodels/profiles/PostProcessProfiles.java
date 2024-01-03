package org.molgenis.emx2.datamodels.profiles;

import com.fasterxml.jackson.databind.util.StdConverter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PostProcessProfiles extends StdConverter<Profiles, Profiles> {

  public static Pattern delimiter = Pattern.compile(",");

  @Override
  public Profiles convert(Profiles profiles) {
    profiles.profileTagsList = csvStringToList(profiles.profileTags);
    profiles.demoDataList = csvStringToList(profiles.demoData);
    profiles.settingsList = csvStringToList(profiles.settings);
    return profiles;
  }

  public static List<String> csvStringToList(String value) {
    return Optional.ofNullable(value)
        .map(delimiter::splitAsStream)
        .map(x -> x.map(String::trim).collect(Collectors.toList()))
        .orElse(Collections.emptyList());
  }
}
