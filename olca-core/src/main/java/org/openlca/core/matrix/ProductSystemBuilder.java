package org.openlca.core.matrix;

import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.matrix.cache.MatrixCache;
import org.openlca.core.matrix.index.LongPair;
import org.openlca.core.matrix.index.ProcessProduct;
import org.openlca.core.matrix.index.TechIndex;
import org.openlca.core.matrix.linking.ITechIndexBuilder;
import org.openlca.core.matrix.linking.LinkingConfig;
import org.openlca.core.matrix.linking.TechIndexBuilder;
import org.openlca.core.matrix.linking.TechIndexCutoffBuilder;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.impl.Constants;
import gnu.trove.set.hash.TLongHashSet;

/**
 * Builds or auto-completes a product system according to a given configuration.
 */
public class ProductSystemBuilder {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private final MatrixCache matrixCache;
	private final LinkingConfig config;

	/**
	 * Create a new product system builder.
	 */
	public ProductSystemBuilder(MatrixCache matrixCache, LinkingConfig config) {
		this.matrixCache = matrixCache;
		this.config = config;
	}

	/**
	 * Creates a new product system for the given process and runs the
	 * auto-complete functions with the linking configuration of this build. The
	 * returned system is not saved to the database.
	 */
	public ProductSystem build(Process process) {
		if (process == null)
			return null;
		ProductSystem system = ProductSystem.of(process);
		autoComplete(system);
		return system;
	}

	/**
	 * Auto-completes the given product system starting with the reference
	 * process of the system and following all product inputs and waste outputs
	 * recursively to link them to a provider process. After this function the
	 * product system will contain an updated set of process IDs and process
	 * links. The meta-data of the product system are not changed. When you then
	 * want to save these updated process IDs and process links in the database
	 * you can call the {@link #update(IDatabase, ProductSystem)} function.
	 */
	public void autoComplete(ProductSystem system) {
		if (system == null
				|| system.referenceExchange == null
				|| system.referenceProcess == null)
			return;
		Process refProcess = system.referenceProcess;
		Flow refProduct = system.referenceExchange.flow;
		if (refProduct == null)
			return;
		ProcessProduct ref = ProcessProduct.of(refProcess, refProduct);
		autoComplete(system, ref);
	}

	/**
	 * Does the same as {@link #autoComplete(ProductSystem)} but starts the
	 * linking at the given process product which can be arbitrary product in
	 * the supply chain of the given system.
	 */
	public void autoComplete(ProductSystem system, ProcessProduct product) {
		log.trace("auto complete product system {}", system);
		log.trace("build product index");
		ITechIndexBuilder builder;
		if (config.cutoff == null || config.cutoff == 0) {
			builder = new TechIndexBuilder(matrixCache, system, config);
		} else {
			builder = new TechIndexCutoffBuilder(
				matrixCache, system, config);
		}
		TechIndex index = builder.build(product);
		log.trace("create new process links");
		addLinksAndProcesses(system, index);
	}

	private void addLinksAndProcesses(ProductSystem system, TechIndex index) {
		TLongHashSet linkIds = new TLongHashSet(Constants.DEFAULT_CAPACITY,
			Constants.DEFAULT_LOAD_FACTOR, -1);
		for (ProcessLink link : system.processLinks) {
			linkIds.add(link.exchangeId);
		}
		for (LongPair exchange : index.getLinkedExchanges()) {
			var provider = index.getLinkedProvider(exchange);
			if (provider == null)
				continue;
			system.processes.add(provider.processId());
			system.processes.add(exchange.first);
			long exchangeId = exchange.second;
			if (linkIds.add(exchangeId)) {
				ProcessLink link = new ProcessLink();
				link.exchangeId = exchangeId;
				link.flowId = provider.flowId();
				link.processId = exchange.first;
				link.providerId = provider.processId();
				system.processLinks.add(link);
			}
		}
	}

	/**
	 * Saves the updated process links and process IDs of the given product system
	 * in the databases. Note that if the product system is already contained in
	 * the database (i.e. has an ID > 0) this function will not update the other
	 * meta-data of the system as it is intended to call this function only for
	 * updating the links and process IDs of a system.
	 */
	public static ProductSystem update(IDatabase db, ProductSystem system) {
		if (system == null)
			return null;
		var dao = new ProductSystemDao(db);
		if (system.id == 0L) {
			return dao.insert(system);
		}
		cleanTables(db, system.id);
		insertLinks(db, system);
		insertProcesses(db, system);
		db.getEntityFactory().getCache().evict(ProductSystem.class);
		return dao.getForId(system.id);
	}

	private static void cleanTables(IDatabase db, long systemId) {
		String sql = "delete from tbl_process_links where "
								 + "f_product_system = " + systemId;
		NativeSql.on(db).runUpdate(sql);
		sql = "delete from tbl_product_system_processes where "
					+ "f_product_system = " + systemId;
		NativeSql.on(db).runUpdate(sql);
	}

	private static void insertLinks(IDatabase db, ProductSystem system) {
		List<ProcessLink> links = system.processLinks;
		String stmt = "insert into tbl_process_links(f_product_system, "
									+ "f_provider, f_process, f_flow, f_exchange) "
									+ "values (?, ?, ?, ?, ?)";
		NativeSql.on(db).batchInsert(stmt, links.size(), (i, statement) -> {
			var link = links.get(i);
			statement.setLong(1, system.id);
			statement.setLong(2, link.providerId);
			statement.setLong(3, link.processId);
			statement.setLong(4, link.flowId);
			statement.setLong(5, link.exchangeId);
			return true;
		});
	}

	private static void insertProcesses(IDatabase db, ProductSystem system) {
		long[] ids = system.processes.stream()
			.mapToLong(Long::longValue).toArray();
		String stmt = "insert into tbl_product_system_processes("
									+ "f_product_system, f_process) values (?, ?)";
		NativeSql.on(db).batchInsert(stmt, ids.length, (i, statement) -> {
			statement.setLong(1, system.id);
			statement.setLong(2, ids[i]);
			return true;
		});
	}
}
