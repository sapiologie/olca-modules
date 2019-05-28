package org.openlca.core.model.descriptors;

import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;

public class FlowDescriptor extends CategorizedDescriptor {

	/** An optional location code of the flow. */
	public String location;
	public FlowType flowType;

	/**
	 * The reference unit of the flow. More correctly, it is the reference unit
	 * of the unit group of the reference flow property of the flow. This is the
	 * unit in which results of this flow are calculated in openLCA.
	 */
	public String refUnit;

	public FlowDescriptor() {
		this.type = ModelType.FLOW;
	}

}
