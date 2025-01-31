package org.openlca.jsonld;

import org.openlca.core.model.ModelType;

public final class ModelPath {

	private ModelPath() {
	}

	/**
	 * Returns the folder where the linked binary files of the data set of the
	 * given type and ID are stored.
	 */
	public static String binFolderOf(ModelType type, String refId) {
		return "bin/" + ModelPath.folderOf(type) + "/" + refId;
	}

	/**
	 * Returns the full path of a Json file that contains a model of the given
	 * type and ID.
	 */
	public static String jsonOf(ModelType type, String refId) {
		return ModelPath.folderOf(type) + "/" + refId + ".json";
	}

	/**
	 * Returns the name of the folder that contains data sets of the given type.
	 */
	public static String folderOf(ModelType type) {
		if (type == null)
			return "unknown";
		return switch (type) {
			case ACTOR -> "actors";
			case CATEGORY -> "categories";
			case CURRENCY -> "currencies";
			case DQ_SYSTEM -> "dq_systems";
			case EPD -> "epds";
			case FLOW -> "flows";
			case FLOW_PROPERTY -> "flow_properties";
			case IMPACT_CATEGORY -> "lcia_categories";
			case IMPACT_METHOD -> "lcia_methods";
			case LOCATION -> "locations";
			case PARAMETER -> "parameters";
			case PROCESS -> "processes";
			case PRODUCT_SYSTEM -> "product_systems";
			case PROJECT -> "projects";
			case RESULT -> "results";
			case SOCIAL_INDICATOR -> "social_indicators";
			case SOURCE -> "sources";
			case UNIT_GROUP -> "unit_groups";
		};
	}

}
