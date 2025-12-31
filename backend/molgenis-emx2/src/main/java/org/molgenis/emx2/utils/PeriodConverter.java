package org.molgenis.emx2.utils;

import java.time.Period;
import org.jooq.Converter;
import org.jooq.types.YearToSecond;

public class PeriodConverter implements Converter<YearToSecond, Period> {

  public PeriodConverter() {}

  @Override
  public Period from(YearToSecond yearToSecond) {
    if (yearToSecond == null) return null;
    return Period.of(yearToSecond.getYears(), yearToSecond.getMonths(), yearToSecond.getDays());
  }

  @Override
  public YearToSecond to(Period period) {
    if (period == null) return null;
    return YearToSecond.valueOf(period);
  }

  @Override
  public Class<YearToSecond> fromType() {
    return YearToSecond.class;
  }

  @Override
  public Class<Period> toType() {
    return Period.class;
  }

  @Override
  public Converter<YearToSecond[], Period[]> forArrays() {
    return Converter.super.forArrays();
  }
}
