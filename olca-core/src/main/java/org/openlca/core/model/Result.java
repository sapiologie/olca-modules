package org.openlca.core.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_results")
public class Result extends CategorizedEntity {

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "f_calculation_setup")
	public CalculationSetup setup;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "f_result")
	public final List<FlowResult> inventory = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "f_result")
	public final List<ImpactResult> impacts = new ArrayList<>();

	/**
	 * The reference flow or quantitative reference of this result. This can be a
	 * product output or waste input. With this flow, a result can be linked as a
	 * provider in a product system to product inputs or waste outputs of other
	 * processes.
	 */
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "f_reference_flow")
	public FlowResult referenceFlow;

	/**
	 * The timestamp when this result was calculated. A value of {@code <= 0}
	 * means that there is not such timestamp.
	 */
	@Column(name = "calculation_time")
	public long calculationTime;

	/**
	 * A URN that points to the origin of the result.
	 */
	@Column(name = "urn")
	public String urn;

	public static Result of(String name) {
		var result = new Result();
		Entities.init(result, name);
		return result;
	}

	@Override
	public Result copy() {
		var clone = new Result();
		Entities.copyRootFields(this, clone);
		clone.urn = urn;
		if (setup != null) {
			clone.setup = setup.copy();
		}
		if (referenceFlow != null) {
			clone.referenceFlow = referenceFlow.copy();
		}
		for (var flow : inventory) {
			clone.inventory.add(flow.copy());
		}
		for (var impact : impacts) {
			clone.impacts.add(impact.copy());
		}
		clone.calculationTime = calculationTime;
		return clone;
	}
}
