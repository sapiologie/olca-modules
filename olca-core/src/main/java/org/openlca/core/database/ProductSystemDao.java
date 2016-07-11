package org.openlca.core.database;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;

public class ProductSystemDao extends
		CategorizedEntityDao<ProductSystem, ProductSystemDescriptor> {

	public ProductSystemDao(IDatabase database) {
		super(ProductSystem.class, ProductSystemDescriptor.class, database);
	}

	public boolean hasReferenceProcess(long id) {
		return hasReferenceProcess(Collections.singleton(id)).get(id);
	}

	public Map<Long, Boolean> hasReferenceProcess(Set<Long> ids) {
		StringBuilder query = new StringBuilder();
		query.append("SELECT id, f_reference_process FROM tbl_product_systems ");
		query.append("WHERE id IN " + asSqlList(ids));
		query.append("AND f_reference_process IN ");
		query.append("(SELECT f_process FROM tbl_product_system_processes WHERE f_process = f_reference_process)");
		Map<Long, Boolean> result = new HashMap<>();
		for (long id : ids)
			result.put(id, false);
		try {
			NativeSql.on(database).query(query.toString(), (res) -> {
				result.put(res.getLong(1), res.getLong(2) != 0l);
				return true;
			});
		} catch (SQLException e) {
			log.error("Error checking for reference process existence", e);
		}
		return result;
	}

	public int countProcessDqSystems(long productSystemId) {
		return countSystems("f_dq_system", productSystemId);
	}

	public int countExchangeDqSystems(long productSystemId) {
		return countSystems("f_exchange_dq_system", productSystemId);
	}

	private int countSystems(String field, long productSystemId) {
		String query = "SELECT count(DISTINCT " + field + ") FROM tbl_processes";
		query += " INNER JOIN tbl_product_system_processes ON tbl_processes.id = tbl_product_system_processes.f_process ";
		query += " WHERE tbl_product_system_processes.f_product_system = " + productSystemId;
		try {
			return NativeSql.on(database).getCount(query);
		} catch (SQLException e) {
			log.error("Error counting process data quality systems", e);
			return 0;
		}
	}

}
