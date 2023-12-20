package org.molgenis.emx2.email;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailValidator {

  private static final Logger logger = LoggerFactory.getLogger(EmailValidator.class);

  public static InternetAddress toInternetAddress(String email) {
    try {
      return new InternetAddress(email);
    } catch (Exception e) {
      throw new MolgenisException("Invalid email address: " + email);
    }
  }

  public static boolean isValidEmail(String email) {
    try {
      new InternetAddress(email);
      return true;
    } catch (AddressException e) {
      return false;
    }
  }

  public static List<String> validationResponseToReceivers(Map<String, Object> validationResponse) {
    List<String> receivers = flattenAllowTree(validationResponse);
    receivers.removeIf((e) -> !EmailValidator.isValidEmail(e));
    return receivers;
  }

  private static List<String> flattenAllowTree(Map<String, Object> resultMap) {
    var asDeepList =
        resultMap.values().stream()
            .map(
                o -> {
                  if (o instanceof Map<?, ?>) {
                    var b = (Map<String, Object>) o;
                    if (b.containsKey("key") && b.containsKey("value")) {
                      return b.get("value");
                    } else {
                      return flattenAllowTree(b);
                    }
                  } else {
                    if (o instanceof List<?>) {
                      return ((List<?>) o)
                          .stream()
                              .map(i -> flattenAllowTree((Map<String, Object>) i))
                              .collect(Collectors.toList());
                    }
                  }
                  return o;
                })
            .toList();

    return flatten(asDeepList);
  }

  private static List<String> flatten(List deepList) {
    var allowedList = new ArrayList<String>();

    for (Object o : deepList) {
      if (o instanceof List<?>) {
        allowedList.addAll(flatten((List) o));
      } else {
        allowedList.add((String) o);
      }
    }
    return allowedList;
  }
}
