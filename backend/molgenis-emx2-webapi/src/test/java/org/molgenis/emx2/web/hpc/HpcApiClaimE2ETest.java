package org.molgenis.emx2.web.hpc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;

/** E2E tests for HPC job claim logic: conflicts, capability matching, and atomic races. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("slow")
class HpcApiClaimE2ETest extends HpcApiTestBase {

  @Test
  @Order(20)
  void claimConflict() {
    String jobId = createJobHelper("conflict-test");

    // Worker A claims
    claimJobHelper(jobId, WORKER_A).then().statusCode(200);

    // Worker B tries to claim -- 409
    claimJobHelper(jobId, WORKER_B).then().statusCode(409).body("title", equalTo("Conflict"));
  }

  @Test
  @Order(21)
  void claimRejectsWorkerWithoutMatchingCapability() {
    String processor = HpcTestkit.nextName("cap-test");
    String profile = "gpu-a";
    String workerBad = HpcTestkit.nextName("cap-bad");
    String workerGood = HpcTestkit.nextName("cap-good");
    registerWorkerWithCapability(workerBad, processor, "gpu-b");
    registerWorkerWithCapability(workerGood, processor, profile);

    String jobId = createJobHelper(processor, profile);

    claimJobHelper(jobId, workerBad)
        .then()
        .statusCode(409)
        .body("title", equalTo("Conflict"))
        .body("detail", containsString("does not have a registered capability"));

    // Job remains claimable by a compatible worker.
    claimJobHelper(jobId, workerGood)
        .then()
        .statusCode(200)
        .body("status", equalTo("CLAIMED"))
        .body("worker_id", equalTo(workerGood));
  }

  @Test
  @Order(22)
  void claimIsAtomicUnderRace() throws Exception {
    String processor = HpcTestkit.nextName("race-test");
    String worker1 = HpcTestkit.nextName("race-worker-1");
    String worker2 = HpcTestkit.nextName("race-worker-2");
    registerWorkerWithCapability(worker1, processor, null);
    registerWorkerWithCapability(worker2, processor, null);
    String jobId = createJobHelper(processor);

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch startLatch = new CountDownLatch(1);
    try {
      Future<Integer> first =
          pool.submit(
              () -> {
                startLatch.await();
                return claimJobHelper(jobId, worker1).statusCode();
              });
      Future<Integer> second =
          pool.submit(
              () -> {
                startLatch.await();
                return claimJobHelper(jobId, worker2).statusCode();
              });

      startLatch.countDown();

      int s1 = first.get(5, TimeUnit.SECONDS);
      int s2 = second.get(5, TimeUnit.SECONDS);
      Assertions.assertTrue(
          (s1 == 200 && s2 == 409) || (s1 == 409 && s2 == 200),
          "Exactly one race claimant must succeed. statuses=" + s1 + "," + s2);

      String winningWorker = s1 == 200 ? worker1 : worker2;
      hpcRequest()
          .when()
          .get("/api/hpc/jobs/{id}", jobId)
          .then()
          .statusCode(200)
          .body("status", equalTo("CLAIMED"))
          .body("worker_id", equalTo(winningWorker));
    } finally {
      pool.shutdownNow();
    }
  }
}
