package org.openlca.core.database;

import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openlca.core.TestSession;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Process;

public class FlowUseQueryTest {

	private ProcessDao processDao = new ProcessDao(
			TestSession.getDefaultDatabase());
	private FlowDao dao = new FlowDao(TestSession.getDefaultDatabase());
	private Flow flow;
	private Process process;

	@Before
	public void setUp() throws Exception {
		flow = new Flow();
		flow.setName("test-flow");
		dao.insert(flow);
		process = new Process();
		process.setName("test-process");
		processDao.insert(process);
	}

	@After
	public void tearDown() throws Exception {
		processDao.delete(process);
		dao.delete(flow);
	}

	@Test
	public void testNotUsed() {
		Set<Long> providerIds = dao.getProviders(flow.getId());
		Assert.assertTrue(providerIds.isEmpty());
		Set<Long> recipientIds = dao.getRecipients(flow.getId());
		Assert.assertTrue(recipientIds.isEmpty());
	}

	@Test
	public void testUsedAsOutput() {
		Exchange exchange = new Exchange();
		exchange.setFlow(flow);
		exchange.setInput(false);
		process.getExchanges().add(exchange);
		processDao.update(process);
		Set<Long> providerIds = dao.getProviders(flow.getId());
		Assert.assertEquals(1, providerIds.size());
		Assert.assertTrue(providerIds.contains(process.getId()));
		Set<Long> recipientIds = dao.getRecipients(flow.getId());
		Assert.assertTrue(recipientIds.isEmpty());
	}

	@Test
	public void testUsedAsInput() {
		Exchange exchange = new Exchange();
		exchange.setFlow(flow);
		exchange.setInput(true);
		process.getExchanges().add(exchange);
		processDao.update(process);
		Set<Long> providerIds = dao.getProviders(flow.getId());
		Assert.assertTrue(providerIds.isEmpty());
		Set<Long> recipientIds = dao.getRecipients(flow.getId());
		Assert.assertEquals(1, recipientIds.size());
		Assert.assertTrue(recipientIds.contains(process.getId()));
	}
}
