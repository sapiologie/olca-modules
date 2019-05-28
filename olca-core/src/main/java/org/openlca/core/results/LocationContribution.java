package org.openlca.core.results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;

/**
 * Calculates the contributions of the single process results in an analysis
 * result grouped by their locations.
 */
public class LocationContribution {

	private ContributionResult result;

	// TODO: using lists of Provider instances could be a bit faster
	private Map<Location, List<CategorizedDescriptor>> index = new HashMap<>();

	public LocationContribution(ContributionResult result, IDatabase db) {
		this.result = result;
		initProcessIndex(db);
	}

	private void initProcessIndex(IDatabase db) {
		if (result == null)
			return;

		Map<Long, Location> locs = new ProcessDao(db).getProcessLocations();
		// group processes by location
		for (CategorizedDescriptor d : result.getProcesses()) {
			Location loc = null;
			if (d instanceof ProcessDescriptor) {
				ProcessDescriptor p = (ProcessDescriptor) d;
				loc = locs.get(p.id);
			}
			List<CategorizedDescriptor> list = index.get(loc);
			if (list == null) {
				list = new ArrayList<>();
				index.put(loc, list);
			}
			list.add(d);
		}
	}

	/** Calculates contributions to an inventory flow. */
	public ContributionSet<Location> calculate(FlowDescriptor flow) {
		if (flow == null || result == null)
			return ContributionSet.empty();
		double total = result.getTotalFlowResult(flow);
		return Contributions.calculate(index.keySet(), total, location -> {
			double amount = 0;
			for (CategorizedDescriptor p : index.get(location)) {
				amount += result.getDirectFlowResult(p, flow);
			}
			return amount;
		});
	}

	/** Calculates contributions to an impact category. */
	public ContributionSet<Location> calculate(
			ImpactCategoryDescriptor impact) {
		if (impact == null || result == null)
			return ContributionSet.empty();
		double total = result.getTotalImpactResult(impact);
		return Contributions.calculate(index.keySet(), total, location -> {
			double amount = 0;
			for (CategorizedDescriptor p : index.get(location)) {
				amount += result.getDirectImpactResult(p, impact);
			}
			return amount;
		});
	}

	/** Calculates added values aggregated by location. */
	public ContributionSet<Location> addedValues() {
		if (result == null)
			return ContributionSet.empty();
		double total = result.totalCosts;
		total = total == 0 ? 0 : -total;
		return Contributions.calculate(index.keySet(), total, location -> {
			double amount = 0;
			for (CategorizedDescriptor p : index.get(location)) {
				double r = result.getDirectCostResult(p);
				r = r == 0 ? 0 : -r;
				amount += r;
			}
			return amount;
		});
	}

	/** Calculates net-costs aggregated by location. */
	public ContributionSet<Location> netCosts() {
		if (result == null)
			return ContributionSet.empty();
		double total = result.totalCosts;
		return Contributions.calculate(index.keySet(), total, location -> {
			double amount = 0;
			for (CategorizedDescriptor p : index.get(location)) {
				amount += result.getDirectCostResult(p);
			}
			return amount;
		});
	}

}
