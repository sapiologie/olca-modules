package org.openlca.core.model.descriptors;

import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProcessType;

public class ProcessDescriptor extends CategorizedDescriptor {

	public ProcessType processType;
	public String location;

	// // TODO: are these fields used, why?
	// public boolean infrastructureProcess;
	// public Long quantitativeReference;

	public ProcessDescriptor() {
		this.type = ModelType.PROCESS;
	}

}
