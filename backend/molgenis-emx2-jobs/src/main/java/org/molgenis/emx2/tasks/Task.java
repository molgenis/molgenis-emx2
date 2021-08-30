package org.molgenis.emx2.tasks;

import static org.molgenis.emx2.tasks.StepStatus.*;

import java.util.*;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Task extends Step implements Runnable, Iterable<Task> {
  private Logger logger = LoggerFactory.getLogger(getClass().getName());
  private List<Step> steps = new ArrayList<>();

  public Task(String message, boolean strict) {
    super(message, strict);
  }

  public Step step(String message) {
    Step step = new Step(message, false);
    this.steps.add(step);
    return step;
  }

  public Step step(String message, StepStatus status) {
    Step step = new Step(message, status, false);
    this.steps.add(step);
    return step;
  }

  public List<Step> getSteps() {
    return steps;
  }

  @Override
  public void run() {}

  public void add(Step task) {
    this.steps.add(task);
  }

  @Override
  public Iterator iterator() {
    return this.steps.iterator();
  }

  @Override
  public void forEach(Consumer action) {
    this.steps.forEach(action);
  }

  @Override
  public Spliterator spliterator() {
    return this.steps.spliterator();
  }
}
