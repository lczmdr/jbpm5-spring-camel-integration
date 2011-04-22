package com.wordpress.lucazamador.jbpm;

import junit.framework.Assert;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringIntegrationTest {

    @Test
    public void springIntegration() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("com/wordpress/lucazamador/jbpm/SpringConfiguration.xml");
        StatefulKnowledgeSession ksession = (StatefulKnowledgeSession) context.getBean("ksession");
        Assert.assertNotNull(ksession);
        ProcessInstance processInstance = ksession.startProcess("org.jbpm.test");
        Assert.assertNotNull(processInstance);
        long processInstance1Id = processInstance.getId();
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, processInstance.getState());
        ksession.signalEvent("continueSignal", null, processInstance1Id);
        Assert.assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
    }

}
