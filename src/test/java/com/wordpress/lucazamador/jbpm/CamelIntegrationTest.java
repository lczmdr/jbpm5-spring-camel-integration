package com.wordpress.lucazamador.jbpm;

import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jndi.JndiContext;
import org.drools.KnowledgeBase;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.command.BatchExecutionCommand;
import org.drools.command.CommandFactory;
import org.drools.command.impl.GenericCommand;
import org.drools.command.runtime.process.SignalEventCommand;
import org.drools.command.runtime.process.StartProcessCommand;
import org.drools.grid.GridNode;
import org.drools.grid.impl.GridImpl;
import org.drools.grid.service.directory.WhitePages;
import org.drools.grid.service.directory.impl.WhitePagesImpl;
import org.drools.io.ResourceFactory;
import org.drools.runtime.ExecutionResults;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CamelIntegrationTest {

    private StatefulKnowledgeSession ksession;

    @Before
    public void configure() {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newClassPathResource("com/wordpress/lucazamador/jbpm/Sample.bpmn"),
                ResourceType.BPMN2);

        if (kbuilder.hasErrors()) {
            if (kbuilder.getErrors().size() > 0) {
                for (KnowledgeBuilderError kerror : kbuilder.getErrors()) {
                    System.err.println(kerror.getMessage());
                }
                throw new RuntimeException(kbuilder.getErrors().toString());
            }
        }

        KnowledgeBase kbase = kbuilder.newKnowledgeBase();

        ksession = kbase.newStatefulKnowledgeSession();
    }

    @After
    public void clean() {
        ksession.dispose();
    }

    @Test
    public void simpleExecution() {
        ProcessInstance processInstance = ksession.startProcess("org.jbpm.test");
        long processInstance1Id = processInstance.getId();
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, processInstance.getState());
        ksession.signalEvent("continueSignal", null, processInstance1Id);
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
    }

    @Test
    public void camelIntegration() throws Exception {
        Context context = configureGridContext(ksession);
        CamelContext camelContext = createCamelContext(context);
        camelContext.start();
        ProducerTemplate template = camelContext.createProducerTemplate();

        StartProcessCommand startProcessCommand = new StartProcessCommand("org.jbpm.test", "process-instance-id");
        List<GenericCommand<?>> commands = new ArrayList<GenericCommand<?>>();
        commands.add(startProcessCommand);
        BatchExecutionCommand batchExecutionCommand = CommandFactory.newBatchExecution(commands, "ksession1");

        ExecutionResults response = (ExecutionResults) template.requestBody("direct:test-with-session",
                batchExecutionCommand);
        Assert.assertNotNull(response);
        Long processInstanceId = (Long) response.getValue("process-instance-id");
        Assert.assertNotNull(processInstanceId);

        SignalEventCommand signalEventCommand = new SignalEventCommand("continueSignal", null);
        commands.clear();
        commands.add(signalEventCommand);
        batchExecutionCommand = CommandFactory.newBatchExecution(commands, "ksession1");
        response = (ExecutionResults) template.requestBody("direct:test-with-session", batchExecutionCommand);
        Assert.assertNotNull(response);

    }

    private Context configureGridContext(StatefulKnowledgeSession ksession) throws Exception {
        GridImpl grid = new GridImpl();
        grid.addService(WhitePages.class, new WhitePagesImpl());
        GridNode node = grid.createGridNode("node");
        Context context = new JndiContext();
        context.bind("node", node);
        node.set("ksession1", ksession);
        return context;
    }

    private CamelContext createCamelContext(Context context) throws Exception {
        CamelContext camelContext = new DefaultCamelContext(context);
        RouteBuilder rb = new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:test-with-session").to("drools://node/ksession1");
            }
        };
        camelContext.addRoutes(rb);
        return camelContext;
    }

}
