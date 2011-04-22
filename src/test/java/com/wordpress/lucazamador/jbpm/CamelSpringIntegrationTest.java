package com.wordpress.lucazamador.jbpm;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.drools.command.BatchExecutionCommand;
import org.drools.command.CommandFactory;
import org.drools.command.impl.GenericCommand;
import org.drools.command.runtime.process.SignalEventCommand;
import org.drools.command.runtime.process.StartProcessCommand;
import org.drools.runtime.ExecutionResults;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CamelSpringIntegrationTest {

    private CamelContext camelContext;
    private ProducerTemplate template;

    @Before
    public void start() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("com/wordpress/lucazamador/jbpm/CamelSpringConfiguration.xml");
        camelContext = (CamelContext) context.getBean("camelContext");        
        template = camelContext.createProducerTemplate();
    }

    @Test
    public void camelSpringIntegration() {
        StartProcessCommand startProcessCommand = new StartProcessCommand("org.jbpm.test", "process-instance-id");
        List<GenericCommand<?>> commands = new ArrayList<GenericCommand<?>>();
        commands.add(startProcessCommand);
        BatchExecutionCommand batchExecutionCommand = CommandFactory.newBatchExecution(commands, "ksession1");

        ExecutionResults response = (ExecutionResults) template.requestBody("direct:test-with-session", batchExecutionCommand);
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

    @After
    public void stop() throws Exception {
        template.stop();
        camelContext.stop();
    }

}
