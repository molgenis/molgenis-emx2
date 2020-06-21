package org.molgenis.emx2.jobs;

/** Immutable */
public class JobProgress {
  private String label;
  private int count = 0;
  private int total = 0;

  public JobProgress(String label, int count, int total) {
    this.label = label;
    this.count = count;
    this.total = total;
  }

  public int getTotal() {
    return total;
  }

  public String getLabel() {
    return label;
  }

  public int getCount() {
    return count;
  }

  public String toString() {
    if (getTotal() > 0)
      return getLabel()
          + ": "
          + getCount()
          + "("
          + Math.round(100 * getCount() / getTotal())
          + "%)";
    else return getLabel() + ": " + getCount();
  }
}
