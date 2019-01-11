package org.openlca.core.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.FlowDescriptor;

import gnu.trove.map.hash.TLongObjectHashMap;

public class FlowDao extends CategorizedEntityDao<Flow, FlowDescriptor> {

	public FlowDao(IDatabase database) {
		super(Flow.class, FlowDescriptor.class, database);
	}

	// start: new descriptor implementation

	@Override
	public List<FlowDescriptor> getDescriptors() {
		String sql = "SELECT id, ref_id, name, description, version, "
				+ "last_change, f_category, flow_type, f_location, "
				+ "f_reference_flow_property FROM tbl_flows";

		ArrayList<FlowDescriptor> list = new ArrayList<>();
		TLongObjectHashMap<String> locations = getLocationCodes();
		try {
			NativeSql.on(database).query(sql, r -> {
				FlowDescriptor d = new FlowDescriptor();
				d.id = r.getLong(1);
				d.refId = r.getString(2);
				d.name = r.getString(3);
				d.description = r.getString(4);
				d.version = r.getLong(5);
				d.lastChange = r.getLong(6);
				d.category = r.getLong(7);
				if (r.wasNull()) {
					d.category = null;
				}
				String fType = r.getString(8);
				if (fType != null) {
					d.flowType = FlowType.valueOf(fType);
				}
				long locationID = r.getLong(9);
				if (!r.wasNull()) {
					d.location = locations.get(locationID);
				}

				return true;
			});
		} catch (Exception e) {
			log.error("Failed to load descriptors", e);
		}

		return list;
	}

	// move this as package private method to the location DAO
	private TLongObjectHashMap<String> getLocationCodes() {
		String sql = "SELECT id, code FROM tbl_locations";
		TLongObjectHashMap<String> m = new TLongObjectHashMap<>();
		try {
			NativeSql.on(database).query(sql, r -> {
				m.put(r.getLong(1), r.getString(2));
				return true;
			});
		} catch (Exception e) {
			log.error("Failed to query location codes", e);
		}
		return m;
	}

	/**
	 * Returns a map that maps the IDs of the flow properties in the database to
	 * the respective reference units.
	 */
	private TLongObjectHashMap<String> getRefUnits() {
		return null;
	}

	// end: new descriptor implementation

	@Override
	protected String[] getDescriptorFields() {
		return new String[] { "id", "ref_id", "name", "description", "version",
				"last_change", "f_category", "flow_type", "f_location",
				"f_reference_flow_property" };
	}

	@Override
	protected FlowDescriptor createDescriptor(Object[] queryResult) {
		if (queryResult == null)
			return null;
		FlowDescriptor descriptor = super.createDescriptor(queryResult);
		if (queryResult[7] instanceof String)
			descriptor.flowType = FlowType.valueOf((String) queryResult[7]);
		descriptor.location = (Long) queryResult[8];
		Long refProp = (Long) queryResult[9];
		if (refProp != null)
			descriptor.refFlowPropertyId = refProp;
		return descriptor;
	}

	/**
	 * Returns the processes where the given flow is an output.
	 */
	public Set<Long> getWhereOutput(long flowId) {
		return getProcessIdsWhereUsed(flowId, false);
	}

	/**
	 * Returns the processes where the given flow is an input.
	 */
	public Set<Long> getWhereInput(long flowId) {
		return getProcessIdsWhereUsed(flowId, true);
	}

	public Set<Long> getUsed() {
		Set<Long> ids = new HashSet<>();
		String query = "SELECT DISTINCT f_flow FROM tbl_exchanges";
		try {
			NativeSql.on(database).query(query, (rs) -> {
				ids.add(rs.getLong("f_flow"));
				return true;
			});
			return ids;
		} catch (Exception e) {
			DatabaseException.logAndThrow(log, "failed to load used flows", e);
			return Collections.emptySet();
		}
	}

	public Set<Long> getReplacementCandidates(long flowId, FlowType type) {
		Set<Long> ids = new HashSet<>();
		String query = "SELECT DISTINCT f_flow FROM tbl_flow_property_factors WHERE f_flow_property IN "
				+ "(SELECT f_flow_property FROM tbl_flow_property_factors WHERE f_flow = "
				+ flowId + ") "
				+ "AND f_flow IN (SELECT DISTINCT id FROM tbl_flows WHERE flow_type = '"
				+ type.name() + "')";
		try {
			NativeSql.on(database).query(query, (rs) -> {
				ids.add(rs.getLong("f_flow"));
				return true;
			});
			ids.remove(flowId);
			return ids;
		} catch (Exception e) {
			DatabaseException.logAndThrow(log,
					"failed to load replacement candidate flows for " + flowId,
					e);
			return Collections.emptySet();
		}
	}

	public void replace(long oldId, long newId,
			boolean excludeExchangesWithProviders) {
		try {
			String statement = null;
			if (excludeExchangesWithProviders) {
				statement = "UPDATE tbl_exchanges SET f_flow = " + newId + " "
						+ "WHERE f_flow = " + oldId
						+ " AND f_default_provider IS NULL";
			} else {
				statement = "UPDATE tbl_exchanges SET f_flow = " + newId
						+ ", f_default_provider = null "
						+ "WHERE f_flow = " + oldId;
			}
			NativeSql.on(database).runUpdate(statement);
			statement = "UPDATE tbl_impact_factors SET f_flow = " + newId
					+ " WHERE f_flow = " + oldId;
			NativeSql.on(database).runUpdate(statement);
		} catch (Exception e) {
			DatabaseException.logAndThrow(log,
					"failed to replace flow " + oldId + " with " + newId, e);
		}
	}

	private Set<Long> getProcessIdsWhereUsed(long flowId, boolean input) {
		Set<Long> ids = new HashSet<>();
		String query = "SELECT f_owner FROM tbl_exchanges WHERE f_flow = "
				+ flowId + " AND is_input = " + (input ? 1 : 0);
		try {
			NativeSql.on(database).query(query, (rs) -> {
				ids.add(rs.getLong("f_owner"));
				return true;
			});
			return ids;
		} catch (Exception e) {
			DatabaseException.logAndThrow(log,
					"failed to load processes for flow " + flowId, e);
			return Collections.emptySet();
		}
	}

	public boolean hasReferenceFactor(long id) {
		return hasReferenceFactor(Collections.singleton(id)).get(id);
	}

	public Map<Long, Boolean> hasReferenceFactor(Set<Long> ids) {
		if (ids == null || ids.isEmpty())
			return new HashMap<>();
		if (ids.size() > MAX_LIST_SIZE)
			return executeChunked2(ids, this::hasReferenceFactor);
		StringBuilder query = new StringBuilder();
		query.append("SELECT id, f_reference_flow_property FROM tbl_flows ");
		query.append("WHERE id IN " + asSqlList(ids));
		query.append(" AND f_reference_flow_property IN ");
		query.append(
				"(SELECT f_flow_property FROM tbl_flow_property_factors WHERE tbl_flows.id = f_flow)");
		Map<Long, Boolean> result = new HashMap<>();
		for (long id : ids)
			result.put(id, false);
		try {
			NativeSql.on(database).query(query.toString(), (res) -> {
				result.put(res.getLong(1), res.getLong(2) != 0l);
				return true;
			});
		} catch (SQLException e) {
			log.error("Error checking for reference factor existence", e);
		}
		return result;
	}

}
