package org.openlca.core.math;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;

import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.MatrixData;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.matrix.index.TechIndex;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.ResultModel;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.IResult;
import org.openlca.core.results.SimpleResult;
import org.openlca.core.results.providers.ResultModelProvider;
import org.openlca.core.results.providers.ResultProviders;

/**
 * Calculates the results of a calculation setup. The product systems of the
 * setups may contain sub-systems which are calculated recursively. The
 * calculator does not check if there are obvious errors like sub-system cycles
 * etc.
 */
public record SystemCalculator(IDatabase db) {

	/**
	 * Calculates the given calculation setup. It performs the calculation type as
	 * defined in the given setup.
	 *
	 * @param setup the calculation setup that should be used
	 * @return the result of the respective calculation
	 */
	public IResult calculate(CalculationSetup setup) {
		if (setup.type() == null)
			return calculateSimple(setup);
		return switch (setup.type()) {
			case SIMPLE_CALCULATION -> calculateSimple(setup);
			case CONTRIBUTION_ANALYSIS -> calculateContributions(setup);
			case UPSTREAM_ANALYSIS -> calculateFull(setup);
			case MONTE_CARLO_SIMULATION -> {
				var simulator = Simulator.create(setup, db, ResultProviders.getSolver());
				for (int i = 0; i < setup.numberOfRuns(); i++) {
					simulator.nextRun();
				}
				yield simulator.getResult();
			}
		};
	}

	public SimpleResult calculateSimple(CalculationSetup setup) {
		return with(setup, matrixData -> SimpleResult.of(db, matrixData));
	}

	public ContributionResult calculateContributions(CalculationSetup setup) {
		return with(setup, matrixData -> ContributionResult.of(db, matrixData));
	}

	public FullResult calculateFull(CalculationSetup setup) {
		return with(setup, matrixData -> FullResult.of(db, matrixData));
	}

	private <T extends SimpleResult> T with(
		CalculationSetup setup, Function<MatrixData, T> fn) {
		var techIndex = TechIndex.of(db, setup);
		var subs = solveSubSystems(setup, techIndex);
		var data = MatrixData.of(db, techIndex)
			.withSetup(setup)
			.withSubResults(subs)
			.build();

		T result = fn.apply(data);

		for (var sub : subs.entrySet()) {
			var provider = sub.getKey();
			var subResult = sub.getValue();
			result.addSubResult(provider, subResult);

			if (provider.isResult()
				&& result.hasImpacts()
				&& subResult.hasImpacts()
				&& !subResult.hasEnviFlows()) {

				var totals = result.totalImpactResults();
				result.impactIndex().each((i, impact) -> {
					var subAmount = subResult.getTotalImpactResult(impact);
					if (subAmount != 0) {
						totals[i] += subAmount;
					}
				});
			}

		}

		return result;
	}

	/**
	 * Calculates (recursively) the sub-systems of the product system of the
	 * given setup. It returns an empty map when there are no subsystems. For
	 * the sub-results, the same calculation type is performed as defined in
	 * the original calculation setup.
	 */
	private Map<TechFlow, SimpleResult> solveSubSystems(
		CalculationSetup setup, TechIndex techIndex) {
		if (setup == null || !setup.hasProductSystem())
			return Collections.emptyMap();

		var subResults = new HashMap<TechFlow, SimpleResult>();

		var subSystems = new HashSet<TechFlow>();
		for (var link : setup.productSystem().processLinks) {
			if (link.hasProcessProvider())
				continue;
			var provider = techIndex.getProvider(link.providerId, link.flowId);
			if (provider == null || provider.isProcess())
				continue;
			if (provider.isProductSystem()) {
				subSystems.add(provider);
				continue;
			}

			// add a result
			if (provider.isResult()) {
				var result = db.get(ResultModel.class, provider.providerId());
				if (result != null) {
					subResults.put(
						provider, new SimpleResult(ResultModelProvider.of(result)));
				}
			}
		}
		if (subSystems.isEmpty())
			return subResults;

		// calculate the LCI results of the sub-systems

		for (var pp : subSystems) {
			var subSystem = db.get(ProductSystem.class, pp.providerId());
			if (subSystem == null)
				continue;

			var subSetup = new CalculationSetup(setup.type(), subSystem)
				.withParameters(ParameterRedefs.join(setup, subSystem))
				.withCosts(setup.hasCosts())
				.withRegionalization(setup.hasRegionalization())
				.withAllocation(setup.allocation())
				.withImpactMethod(setup.impactMethod())
				.withNwSet(setup.nwSet());
			var subResult = calculate(subSetup);
			if (subResult instanceof SimpleResult simple) {
				subResults.put(pp, simple);
			}
		}
		return subResults;
	}
}
