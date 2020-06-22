package org.molgenis.emx2.jobs;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class TestJobs {

  @Test
  public void test1() throws InterruptedException {
    JobService js = new JobServiceInMemory();

    Job j1 = new DummyJob();

    String id = js.add(j1);
    System.out.println("Starting ...");

    // purge doesn't change
    js.removeBeforeTime(LocalDateTime.now());
    Assert.assertEquals(1, js.getJobIds().size());

    while (!JobStatus.COMPLETED.equals(js.getJob(id).getStatus())) {
      Thread.sleep(50);
      for (JobTask jp : js.getJob(id).getTasks()) {
        System.out.println(jp);
      }
    }
    System.out.println("Completed ...");
    for (JobTask jp : js.getJob(id).getTasks()) {
      System.out.println(jp);
    }

    Assert.assertEquals(1, js.getJobIds().size());

    // purge after complete removes
    js.removeBeforeTime(LocalDateTime.now());
    Assert.assertEquals(0, js.getJobIds().size());

    js.shutdown();
  }
}
