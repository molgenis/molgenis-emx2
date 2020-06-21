package org.molgenis.emx2.jobs;

import org.junit.Assert;
import org.junit.Test;

public class TestJobs {

  @Test
  public void test1() throws InterruptedException {
    JobService js = new JobServiceInMemory();

    Job j1 = new DummyJob();

    String id = js.add(j1);
    System.out.println("Starting ...");

    // purge doesn't change
    js.purge();
    Assert.assertEquals(1, js.getJobs().size());

    while (!JobStatus.COMPLETED.equals(js.getStatus(id))) {
      Thread.sleep(50);
      for (JobProgress jp : js.getProgress(id)) {
        System.out.print(jp);
      }
      System.out.flush();
      System.out.print("\r");
    }
    System.out.println("Completed ...");
    for (JobProgress jp : js.getCompleted(id)) {
      System.out.println(jp);
    }

    Assert.assertEquals(1, js.getJobs().size());

    // purge after complete removes
    js.purge();
    Assert.assertEquals(0, js.getJobs().size());

    js.shutdown();
  }
}
