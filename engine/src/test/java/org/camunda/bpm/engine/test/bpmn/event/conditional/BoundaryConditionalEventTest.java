/*
 * Copyright 2016 camunda services GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.test.bpmn.event.conditional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.camunda.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.camunda.bpm.engine.test.bpmn.event.conditional.EventSubProcessStartConditionalEventTest.TASK_AFTER_SERVICE_TASK;

import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractActivityBuilder;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public class BoundaryConditionalEventTest extends AbstractConditionalEventTestCase {

  protected static final String TASK_WITH_CONDITION = "Task with condition";
  protected static final String TASK_WITH_CONDITION_ID = "taskWithCondition";
  protected static final String TASK_IN_SUBPROCESS = "Task in Subprocess";

  // TODO: test process instance can complete successfully

  @Test
  @Deployment
  public void testTrueCondition() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when variable is set on task execution
    runtimeService.setVariable(task.getExecutionId(), VARIABLE_NAME, 1);

    //then execution is at user task after boundary event
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());

    //and subscriptions are cleaned up
    assertEquals(0, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  @Deployment
  public void testNonInterruptingTrueCondition() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when variable is set on task execution
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after boundary event and in the task with the boundary event
    List<Task> tasklist = taskQuery.list();
    assertEquals(2, tasklist.size());

    //and subscriptions are still exist
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  @Deployment
  public void testFalseCondition() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery();
    Task task = taskQuery.processInstanceId(procInst.getId()).singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());

    //when variable is set on task execution
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution stays in task with boundary condition
    Execution execution = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .activityId(TASK_WITH_CONDITION_ID)
            .singleResult();
    assertNotNull(execution);
  }

  @Test
  @Deployment
  public void testVariableCondition() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());

    //when local variable is set on task with condition
    taskService.setVariableLocal(task.getId(), VARIABLE_NAME, 1);

    //then execution should remain on task
    Execution execution = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .activityId(TASK_WITH_CONDITION_ID)
            .singleResult();
    assertNotNull(execution);
  }

  @Test
  @Deployment(resources = {"org/camunda/bpm/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testVariableCondition.bpmn20.xml"})
  public void testVariableSetOnExecutionCondition() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());

    //when variable is set on task execution
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution ends
    Execution execution = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .activityId(TASK_WITH_CONDITION_ID)
            .singleResult();
    assertNull(execution);

    //and execution is at user task after boundary event
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  @Deployment
  public void testNonInterruptingVariableCondition() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());

    //when variable is set on task with condition
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after boundary event and in the task with the boundary event
    List<Task> tasklist = taskQuery.list();
    assertEquals(2, tasklist.size());
  }

  @Test
  @Deployment(resources = {"org/camunda/bpm/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testVariableCondition.bpmn20.xml"})
  public void testWrongVariableCondition() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when wrong variable is set on task execution
    taskService.setVariable(task.getId(), VARIABLE_NAME + 1, 1);

    //then execution stays at user task with condition
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when correct variable is set
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is on user task after condition
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
    assertEquals(0, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  @Deployment
  public void testParallelVariableCondition() {
    //given process with parallel user tasks and boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
    assertEquals(2, conditionEventSubscriptionQuery.list().size());

    Task task = tasks.get(0);

    //when local variable is set on task
    taskService.setVariableLocal(task.getId(), VARIABLE_NAME, 1);

    //then nothing happens
    tasks = taskQuery.list();
    assertEquals(2, tasks.size());

    //when local variable is set on task execution
    runtimeService.setVariableLocal(task.getExecutionId(), VARIABLE_NAME, 1);

    //then boundary event is triggered of this task and task ends (subscription is deleted)
    //other execution stays in other task
    List<Execution> executions = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .list();
    assertEquals(2, executions.size());

    tasks = taskQuery.list();
    assertEquals(1, tasks.size());

    Execution execution = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .activityId(task.getId())
            .singleResult();
    assertNull(execution);
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  @Deployment(resources = {"org/camunda/bpm/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testParallelVariableCondition.bpmn20.xml"})
  public void testParallelSetVariableOnTaskCondition() {
    //given process with parallel user tasks and boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());

    Task task = tasks.get(0);

    //when variable is set on execution
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then both boundary event are triggered and process instance ends
    List<Execution> executions = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .list();
    assertEquals(0, executions.size());

    tasks = taskQuery.list();
    assertEquals(0, tasks.size());
  }

  @Test
  @Deployment(resources = {"org/camunda/bpm/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testParallelVariableCondition.bpmn20.xml"})
  public void testParallelSetVariableOnExecutionCondition() {
    //given process with parallel user tasks and boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());

    //when variable is set on execution
    //taskService.setVariable(task.getId(), VARIABLE_NAME, 1);
    runtimeService.setVariable(procInst.getId(), VARIABLE_NAME, 1);

    //then both boundary events are triggered and process instance ends
    List<Execution> executions = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .list();
    assertEquals(0, executions.size());

    tasks = taskQuery.list();
    assertEquals(0, tasks.size());
  }

  @Test
  @Deployment
  public void testSubProcessVariableCondition() {
    //given process with boundary conditional event on sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_IN_SUBPROCESS, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when local variable is set on task with condition
    taskService.setVariableLocal(task.getId(), VARIABLE_NAME, 1);

    //then execution stays on user task
    List<Execution> executions = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .list();
    assertEquals(2, executions.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when local variable is set on task execution
    runtimeService.setVariableLocal(task.getExecutionId(), VARIABLE_NAME, 1);

    //then process instance ends
    executions = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .list();
    assertEquals(0, executions.size());
  }

  @Test
  @Deployment(resources = {"org/camunda/bpm/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testSubProcessVariableCondition.bpmn20.xml"})
  public void testSubProcessSetVariableOnTaskCondition() {
    //given process with boundary conditional event on sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_IN_SUBPROCESS, task.getName());

    //when variable is set on task execution with condition
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then process instance ends
    List<Execution> executions = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .list();
    assertEquals(0, executions.size());
  }

  @Test
  @Deployment(resources = {"org/camunda/bpm/engine/test/bpmn/event/conditional/BoundaryConditionalEventTest.testSubProcessVariableCondition.bpmn20.xml"})
  public void testSubProcessSetVariableOnExecutionCondition() {
    //given process with boundary conditional event on sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_IN_SUBPROCESS, task.getName());

    //when variable is set on task execution with condition
    runtimeService.setVariable(task.getExecutionId(), VARIABLE_NAME, 1);

    //then process instance ends
    List<Execution> executions = runtimeService.createExecutionQuery()
            .processInstanceId(procInst.getId())
            .list();
    assertEquals(0, executions.size());
  }

  @Test
  @Deployment
  public void testNonInterruptingSubProcessVariableCondition() {
    //given process with boundary conditional event on sub process
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_IN_SUBPROCESS, task.getName());

    //when variable is set on task with condition
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution stays on user task and is on task after condition
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
  }

  @Test
  @Deployment
  public void testCleanUpConditionalEventSubscriptions() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();

    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when task is completed
    taskService.complete(task.getId());

    //then conditional subscription should be deleted
    assertEquals(0, conditionEventSubscriptionQuery.list().size());
  }


  protected void deployBoundaryEventProcessWithVariableIsSetInDelegationCode(BpmnModelInstance model, boolean isInterrupting) {
    final BpmnModelInstance modelInstance = modify(model)
            .serviceTaskBuilder(TASK_WITH_CONDITION_ID)
            .boundaryEvent()
            .cancelActivity(isInterrupting)
            .conditionalEventDefinition(CONDITIONAL_EVENT)
            .condition(CONDITION_EXPR)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent()
            .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());
  }

  protected void deployBoundaryEventProcess(AbstractActivityBuilder builder, boolean isInterrupting) {
    deployBoundaryEventProcess(builder, CONDITION_EXPR, isInterrupting);
  }

  protected void deployBoundaryEventProcess(AbstractActivityBuilder builder, String conditionExpr, boolean isInterrupting) {
    final BpmnModelInstance modelInstance = builder
            .boundaryEvent()
            .cancelActivity(isInterrupting)
            .conditionalEventDefinition(CONDITIONAL_EVENT)
            .condition(conditionExpr)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent()
            .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());
  }


  @Test
  public void testSetVariableInDelegate() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .camundaClass(SetVariableDelegate.class.getName())
                                                  .endEvent().done();
    deployBoundaryEventProcessWithVariableIsSetInDelegationCode(modelInstance, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then service task with delegated code is called and variable is set
    //-> conditional event is triggered and execution stays at user task after condition
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
    assertEquals(0, conditionEventSubscriptionQuery.list().size());
  }


  @Test
  public void testNonInterruptingSetVariableInDelegate() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .camundaClass(SetVariableDelegate.class.getName())
                                                  .userTask()
                                                  .endEvent().done();
    deployBoundaryEventProcessWithVariableIsSetInDelegationCode(modelInstance, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task before service task is completed
    taskService.complete(task.getId());

    //then service task with delegated code is called and variable is set
    //-> non interrupting conditional event is triggered
    //execution stays at user task after condition and after service task
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());

    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());
    assertEquals(0, engine.getRuntimeService().createProcessInstanceQuery().count());
  }


  @Test
  public void testSetVariableInInputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .camundaInputParameter(VARIABLE_NAME, "1")
                                                    .camundaExpression(TRUE_CONDITION)
                                                  .endEvent().done();
    deployBoundaryEventProcessWithVariableIsSetInDelegationCode(modelInstance, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    // input mapping does not trigger boundary event and process ends regularly
    assertEquals(0, engine.getRuntimeService().createProcessInstanceQuery().count());
  }


  @Test
  public void testNonInterruptingSetVariableInInputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .camundaInputParameter(VARIABLE_NAME, "1")
                                                    .camundaExpression(TRUE_CONDITION)
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent().done();
    deployBoundaryEventProcessWithVariableIsSetInDelegationCode(modelInstance, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task before service task is completed
    taskService.complete(task.getId());

    // then the variable is set in an input mapping
    // -> non interrupting conditional event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_SERVICE_TASK, task.getName());
  }


  @Test
  public void testSetVariableInExpression() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent().userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .camundaExpression(EXPR_SET_VARIABLE)
                                                  .endEvent().done();
    deployBoundaryEventProcessWithVariableIsSetInDelegationCode(modelInstance, true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then service task with expression is called and variable is set
    //-> interrupting conditional event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInExpression() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask().name(TASK_BEFORE_CONDITION)
                                                  .serviceTask(TASK_WITH_CONDITION_ID)
                                                    .camundaExpression(EXPR_SET_VARIABLE)
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent().done();
    deployBoundaryEventProcessWithVariableIsSetInDelegationCode(modelInstance, false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task before service task is completed
    taskService.complete(task.getId());

    //then service task with expression is called and variable is set
    //->non interrupting conditional event is triggered
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
  }

  @Test
  public void testSetVariableInInputMappingOfSubProcess() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask().name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUBPROCESS_ID)
                                                    .camundaInputParameter(VARIABLE_NAME, "1")
                                                    .embeddedSubProcess()
                                                    .startEvent()
                                                    .userTask().name(TASK_IN_SUB_PROCESS)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(SUBPROCESS_ID), true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    // Then input mapping from sub process sets variable, but
    // interrupting conditional event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_IN_SUB_PROCESS, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testNonInterruptingSetVariableInInputMappingOfSubProcess() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask().name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUBPROCESS_ID)
                                                    .camundaInputParameter(VARIABLE_NAME, "1")
                                                    .embeddedSubProcess()
                                                    .startEvent()
                                                    .userTask().name(TASK_IN_SUB_PROCESS)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(SUBPROCESS_ID), false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task before is completed
    taskService.complete(task.getId());

    // Then input mapping from sub process sets variable, but
    // non interrupting conditional event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_IN_SUB_PROCESS, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }


  @Test
  public void testSetVariableInOutputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                    .camundaOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask().name("afterOutputMapping")
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_BEFORE_CONDITION_ID), true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then output mapping sets variable
    //boundary event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals("afterOutputMapping", task.getName());
  }


  @Test
  public void testSetVariableInOutputMappingWithBoundary() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                    .camundaOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_WITH_CONDITION_ID), true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then output mapping sets variable
    //boundary event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInOutputMapping() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                    .camundaOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_WITH_CONDITION_ID), false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task with output mapping is completed
    taskService.complete(task.getId());

    //then output mapping sets variable
    //boundary event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());
  }


  @Test
  public void testNonInterruptingSetVariableInOutputMappingWithBoundary() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_WITH_CONDITION_ID)
                                                    .name(TASK_WITH_CONDITION)
                                                    .camundaOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask().name("afterOutputMapping")
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_WITH_CONDITION_ID), false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());

    //when task with output mapping is completed
    taskService.complete(task.getId());

    //then output mapping sets variable
    //boundary event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals("afterOutputMapping", task.getName());
  }


  @Test
  public void testSetVariableInOutputMappingOfCallActivity() {
    final BpmnModelInstance delegatedInstance = Bpmn.createExecutableProcess("delegatedProcess")
                                                     .startEvent()
                                                     .serviceTask()
                                                     .camundaExpression(EXPR_SET_VARIABLE)
                                                     .endEvent()
                                                     .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, delegatedInstance).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .callActivity(TASK_WITH_CONDITION_ID)
                                                    .calledElement("delegatedProcess")
                                                    .camundaOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask("afterOutputMapping")
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_WITH_CONDITION_ID), true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then output mapping from call activity sets variable
    //-> interrupting conditional event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals("afterOutputMapping", task.getTaskDefinitionKey());
  }

  @Test
  public void testNonInterruptingSetVariableInOutputMappingOfCallActivity() {
    final BpmnModelInstance delegatedInstance = Bpmn.createExecutableProcess("delegatedProcess")
                                                     .startEvent()
                                                     .serviceTask()
                                                     .camundaExpression(EXPR_SET_VARIABLE)
                                                     .endEvent()
                                                     .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, delegatedInstance).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .callActivity(TASK_WITH_CONDITION_ID)
                                                    .calledElement("delegatedProcess")
                                                    .camundaOutputParameter(VARIABLE_NAME, "1")
                                                  .userTask().name("afterOutputMapping")
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_WITH_CONDITION_ID), false);


    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task before service task is completed
    taskService.complete(task.getId());

    //then out mapping of call activity sets a variable
    //-> non interrupting conditional event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals("afterOutputMapping", task.getName());
  }

  @Test
  public void testSetVariableInStartListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
                                                    .camundaExecutionListenerExpression(ExecutionListener.EVENTNAME_START, EXPR_SET_VARIABLE)
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_WITH_CONDITION_ID), true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //boundary event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInStartListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
                                                    .camundaExecutionListenerExpression(ExecutionListener.EVENTNAME_START, EXPR_SET_VARIABLE)
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_WITH_CONDITION_ID), false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //non interrupting boundary event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());
  }

  @Test
  public void testSetVariableInTakeListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .sequenceFlowId(FLOW_ID)
                                                  .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
                                                  .endEvent()
                                                  .done();
    CamundaExecutionListener listener = modelInstance.newInstance(CamundaExecutionListener.class);
    listener.setCamundaEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setCamundaExpression(EXPR_SET_VARIABLE);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_WITH_CONDITION_ID), true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then take listener sets variable
    //conditional event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInTakeListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .sequenceFlowId(FLOW_ID)
                                                  .userTask(TASK_WITH_CONDITION_ID).name(TASK_WITH_CONDITION)
                                                  .endEvent()
                                                  .done();
    CamundaExecutionListener listener = modelInstance.newInstance(CamundaExecutionListener.class);
    listener.setCamundaEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setCamundaExpression(EXPR_SET_VARIABLE);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_WITH_CONDITION_ID), false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then take listener sets variable
    //non interrupting boundary event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());
  }

  @Test
  public void testSetVariableInEndListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                    .camundaExecutionListenerExpression(ExecutionListener.EVENTNAME_END, EXPR_SET_VARIABLE)
                                                  .userTask(TASK_WITH_CONDITION_ID)
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_BEFORE_CONDITION_ID), true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //conditional event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInEndListener() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                    .camundaExecutionListenerExpression(ExecutionListener.EVENTNAME_END, EXPR_SET_VARIABLE)
                                                  .userTask(TASK_WITH_CONDITION_ID)
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_BEFORE_CONDITION_ID), false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //non interrupting boundary event is triggered
    List<Task> tasks = taskQuery.list();
    assertEquals(1, tasks.size()); // TODO: end listener should not be able to trigger boundary event
    assertEquals(TASK_WITH_CONDITION_ID, tasks.get(0).getTaskDefinitionKey());
  }

  @Test
  public void testSetVariableInMultiInstance() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .userTask(TASK_WITH_CONDITION_ID)
                                                  .multiInstance()
                                                    .cardinality("3")
                                                    .parallel()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_WITH_CONDITION_ID), "${nrOfInstances == 3}", true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then multi instance is created
    //and boundary event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInMultiInstance() {
    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .userTask(TASK_WITH_CONDITION_ID)
                                                  .multiInstance()
                                                    .cardinality("3")
                                                    .parallel()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_WITH_CONDITION_ID), "${nrOfInstances == 3}", false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //non interrupting boundary event is triggered
    List<Task> tasks = taskQuery.list();
    assertEquals(6, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }


  @Test
  public void testSetVariableInCallActivity() {
    final BpmnModelInstance delegatedInstance = Bpmn.createExecutableProcess("delegatedProcess")
                                                     .startEvent()
                                                     .serviceTask()
                                                     .camundaExpression(EXPR_SET_VARIABLE)
                                                     .endEvent()
                                                     .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, delegatedInstance).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .callActivity(TASK_WITH_CONDITION_ID)
                                                    .calledElement("delegatedProcess")
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_WITH_CONDITION_ID), true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then service task in call activity sets variable
    //conditional event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_SERVICE_TASK, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInCallActivity() {
    final BpmnModelInstance delegatedInstance = Bpmn.createExecutableProcess("delegatedProcess")
                                                     .startEvent()
                                                     .serviceTask()
                                                     .camundaExpression(EXPR_SET_VARIABLE)
                                                     .endEvent()
                                                     .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, delegatedInstance).deploy());

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .callActivity(TASK_WITH_CONDITION_ID)
                                                    .calledElement("delegatedProcess")
                                                  .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(TASK_WITH_CONDITION_ID), false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then service task in call activity sets variable
    //conditional event is not triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_SERVICE_TASK, task.getName());
  }

  @Test
  public void testSetVariableInSubProcessInDelegatedCode() {

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUBPROCESS_ID)
                                                  .embeddedSubProcess()
                                                    .startEvent()
                                                    .serviceTask()
                                                    .camundaExpression(EXPR_SET_VARIABLE)
                                                    .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(SUBPROCESS_ID), true);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then service task in sub process sets variable
    //conditional event is triggered
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());
  }

  @Test
  public void testNonInterruptingSetVariableInSubProcessInDelegatedCode() {

    final BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
                                                  .startEvent()
                                                  .userTask(TASK_BEFORE_CONDITION_ID)
                                                    .name(TASK_BEFORE_CONDITION)
                                                  .subProcess(SUBPROCESS_ID)
                                                  .embeddedSubProcess()
                                                    .startEvent()
                                                    .serviceTask()
                                                    .camundaExpression(EXPR_SET_VARIABLE)
                                                    .userTask().name(TASK_AFTER_SERVICE_TASK)
                                                    .endEvent()
                                                  .subProcessDone()
                                                  .endEvent()
                                                  .done();
    deployBoundaryEventProcess(modify(modelInstance).activityBuilder(SUBPROCESS_ID), false);

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then service task in sub process sets variable
    //conditional event is triggered
    List<Task> tasks = taskQuery.list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  // variable name /////////////////////////////////////////////////////////////

  @Test
  public void testVariableConditionWithVariableName() {

    //given process with boundary conditional event and defined variable name
    deployBoundaryEventProcessWithVarName(true);

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);

    //when variable with name `variable1` is set on execution
    taskService.setVariable(task.getId(), VARIABLE_NAME + 1, 1);

    //then nothing happens
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when variable with name `variable` is set on execution
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional start event
    task = taskQuery.singleResult();
    assertEquals(TASK_AFTER_CONDITION, task.getName());
    assertEquals(0, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testVariableConditionWithVariableNameAndEvent() {

    //given process with boundary conditional event and defined variable name and event
    deployBoundaryEventProcessWithVarNameAndEvents(true, CONDITIONAL_VAR_EVENT_UPDATE);

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);

    //when variable with name `variable` is set on execution
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then nothing happens
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when variable with name `variable` is updated
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1);

    //then execution is at user task after conditional start event
    task = taskQuery.singleResult();
    assertEquals(TASK_AFTER_CONDITION, task.getName());
    assertEquals(0, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testNonInterruptingVariableConditionWithVariableName() {

    //given process with non interrupting boundary conditional event and defined variable name
    deployBoundaryEventProcessWithVarName(false);

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);

    //when variable with name `variable1` is set on execution
    taskService.setVariable(task.getId(), VARIABLE_NAME + 1, 1);

    //then nothing happens
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when variable with name `variable` is set, updated and deleted
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //create
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //update
    taskService.removeVariable(task.getId(), VARIABLE_NAME); //delete

    //then execution is for three times at user task after conditional start event
    List<Task> tasks = taskQuery.taskName(TASK_AFTER_CONDITION).list();
    assertEquals(3, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testNonInterruptingVariableConditionWithVariableNameAndEvents() {

    //given process with non interrupting boundary conditional event and defined variable name and events
    deployBoundaryEventProcessWithVarNameAndEvents(false, CONDITIONAL_VAR_EVENTS);

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);

    //when variable with name `variable` is set, updated and deleted
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //create
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //update
    taskService.removeVariable(task.getId(), VARIABLE_NAME); //delete

    //then execution is for two times at user task after conditional start event
    List<Task> tasks = taskQuery.taskName(TASK_AFTER_CONDITION).list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testSetMultipleVariables() {

    // given
    BpmnModelInstance modelInstance = modify(TASK_MODEL)
        .userTaskBuilder(TASK_BEFORE_CONDITION_ID)
        .boundaryEvent()
          .cancelActivity(true)
          .conditionalEventDefinition("event1")
          .condition("${variable1 == 1}")
          .conditionalEventDefinitionDone()
        .userTask("afterBoundary1")
        .endEvent()
        .moveToActivity(TASK_BEFORE_CONDITION_ID)
        .boundaryEvent()
          .cancelActivity(true)
          .conditionalEventDefinition("event2")
          .condition("${variable2 == 1}")
          .conditionalEventDefinitionDone()
        .userTask("afterBoundary2")
        .endEvent()
        .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    ProcessInstance processInstance = engine.getRuntimeService().startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY, Variables.createVariables().putValue("variable1", "44").putValue("variable2", "44"));

    Task task = engine.getTaskService().createTaskQuery().singleResult();

    // when
    taskService.setVariables(task.getId(), Variables
        .createVariables()
        .putValue("variable1", 1)
        .putValue("variable2", 1));

    // then
    assertEquals(1, engine.getTaskService().createTaskQuery().count());
    Task afterBoundary1Task = engine.getTaskService().createTaskQuery().singleResult();
    String taskDefinitionKey = afterBoundary1Task.getTaskDefinitionKey();
    Assert.assertTrue("afterBoundary1".equals(taskDefinitionKey) || "afterBoundary2".equals(taskDefinitionKey));

  }

  @Test
  public void testVariableConditionWithVariableEvent() {

    //given process with boundary conditional event and defined variable event
    deployBoundaryEventProcessWithVarEvents(true, CONDITIONAL_VAR_EVENT_UPDATE);

    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME + 1, 0);
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY, variables);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);

    //when variable with name `variable` is set on execution
    runtimeService.setVariable(procInst.getId(), VARIABLE_NAME, 1);

    //then nothing happens
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when variable with name `variable1` is updated
    runtimeService.setVariable(procInst.getId(), VARIABLE_NAME + 1, 1);

    //then execution is at user task after conditional intermediate event
    task = taskQuery.singleResult();
    assertEquals(TASK_AFTER_CONDITION, task.getName());
    assertEquals(0, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testNonInterruptingVariableConditionWithVariableEvent() {

    //given process with non interrupting boundary conditional event and defined variable event
    deployBoundaryEventProcessWithVarEvents(false, CONDITIONAL_VAR_EVENT_UPDATE);

    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);

    //when variable with name `variable` is set
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //create

    //then nothing happens
    task = taskQuery.singleResult();
    assertNotNull(task);

    //when variable is updated twice
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //update
    taskService.setVariable(task.getId(), VARIABLE_NAME, 1); //update

    //then execution is for two times at user task after conditional start event
    List<Task> tasks = taskQuery.taskName(TASK_AFTER_CONDITION).list();
    assertEquals(2, tasks.size());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());
  }

  @Test
  @Deployment
  public void testTrueConditionWithExecutionListener() {
    //given process with boundary conditional event
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_WITH_CONDITION, task.getName());
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when task is completed the execution listener is called which sets a variable (equal to output mapping)
    taskService.complete(task.getId());

    //then condition will be evaluated, since the execution of the user task still exists (expected behavior)
    task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_AFTER_CONDITION, task.getName());

    //and subscriptions are cleaned up
    assertEquals(0, conditionEventSubscriptionQuery.list().size());
  }

  protected void deployBoundaryEventProcessWithVarName(boolean interrupting) {
    final BpmnModelInstance modelInstance = modify(TASK_MODEL)
            .userTaskBuilder(TASK_BEFORE_CONDITION_ID)
            .boundaryEvent()
            .cancelActivity(interrupting)
            .conditionalEventDefinition(CONDITIONAL_EVENT)
            .condition(TRUE_CONDITION)
            .camundaVariableName(VARIABLE_NAME)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent()
            .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());
  }

  protected void deployBoundaryEventProcessWithVarNameAndEvents(boolean interrupting, String varEvent) {
    final BpmnModelInstance modelInstance = modify(TASK_MODEL)
            .userTaskBuilder(TASK_BEFORE_CONDITION_ID)
            .boundaryEvent()
            .cancelActivity(interrupting)
            .conditionalEventDefinition(CONDITIONAL_EVENT)
            .condition(CONDITION_EXPR)
            .camundaVariableName(VARIABLE_NAME)
            .camundaVariableEvents(varEvent)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent()
            .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());
  }

  protected void deployBoundaryEventProcessWithVarEvents(boolean interrupting, String varEvent) {
    final BpmnModelInstance modelInstance = modify(TASK_MODEL)
            .userTaskBuilder(TASK_BEFORE_CONDITION_ID)
            .boundaryEvent()
            .cancelActivity(interrupting)
            .conditionalEventDefinition(CONDITIONAL_EVENT)
            .condition(CONDITION_EXPR)
            .camundaVariableEvents(varEvent)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent()
            .done();

    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());
  }
}
