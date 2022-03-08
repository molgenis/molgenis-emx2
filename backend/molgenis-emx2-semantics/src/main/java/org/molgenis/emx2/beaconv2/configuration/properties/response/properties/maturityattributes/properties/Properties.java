package org.molgenis.emx2.beaconv2.configuration.properties.response.properties.maturityattributes.properties;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.configuration.properties.response.properties.maturityattributes.properties.productionstatus.ProductionStatus;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Properties {
  ProductionStatus productionStatus = new ProductionStatus();
}
