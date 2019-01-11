package org.openlca.core.model.descriptors;

import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;

public class FlowDescriptor extends CategorizedDescriptor {

	/**
	 * The name or code of the location of the flow. Typically, this is only
	 * used for product flows in databases like ecoinvent.
	 */
	public String location;

	/**
	 * The specific flow type (product, waste, or elementary flow).
	 */
	public FlowType flowType;

	/**
	 * The reference unit of the flow (useful for the display of calculation
	 * results with flow meta-data).
	 */
	public String unit;

	public FlowDescriptor() {
		this.type = ModelType.FLOW;
	}

}
