package org.openlca.core.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.core.results.EnviFlowValue;
import org.openlca.core.results.ImpactValue;
import org.openlca.jsonld.Json;
import org.openlca.jsonld.output.DbRefs;

import java.util.function.Function;

final class JsonUtil {

	private JsonUtil() {
	}

	static JsonObject encodeTechFlow(TechFlow techFlow, DbRefs refs) {
		if (techFlow == null)
			return null;
		var obj = new JsonObject();
		Json.put(obj, "provider", refs.asRef(techFlow.provider()));
		Json.put(obj, "flow", refs.asRef(techFlow.flow()));
		return obj;
	}

	static JsonObject encodeEnviFlow(EnviFlow enviFlow, DbRefs refs) {
		if (enviFlow == null)
			return null;
		var obj = new JsonObject();
		Json.put(obj, "flow", refs.asRef(enviFlow.flow()));
		if (enviFlow.location() != null) {
			Json.put(obj, "location", refs.asRef(enviFlow.location()));
		}
		Json.put(obj, "isInput", enviFlow.isInput());
		if (enviFlow.isVirtual()) {
			Json.put(obj, "isVirtual", true);
			if (enviFlow.wrapped() instanceof RootDescriptor wrapped) {
				Json.put(obj, "wrapped", refs.asRef(wrapped));
			}
		}
		return obj;
	}

	static JsonObject encodeState(CalculationQueue.State state) {
		var obj = new JsonObject();
		if (state == null || state.isEmpty()) {
			Json.put(obj, "error", "does not exist");
			return obj;
		}
		Json.put(obj, "@id", state.id());
		if (state.isError()) {
			Json.put(obj, "error", state.error());
			return obj;
		}
		Json.put(obj, "isReady", state.isReady());
		Json.put(obj, "isScheduled", state.isScheduled());
		Json.put(obj, "time", state.time());
		return obj;
	}

	static JsonObject encodeImpactValue(ImpactValue v, DbRefs refs) {
		if (v == null)
			return null;
		var obj = new JsonObject();
		obj.add("impactCategory", refs.asRef(v.impact()));
		obj.addProperty("value", v.value());
		return obj;
	}

	static JsonObject encodeFlowValue(EnviFlowValue v, DbRefs refs) {
		if (v == null)
			return null;
		var obj = new JsonObject();
		Json.put(obj, "enviFlow", encodeEnviFlow(v.enviFlow(), refs));
		Json.put(obj, "value", v.value());
		return obj;
	}

	static  <T> JsonArray encodeArray(
			Iterable<T> items, Function<T, JsonObject> fn) {
		var array = new JsonArray();
		for (var next : items) {
			var json = fn.apply(next);
			if (json != null) {
				array.add(json);
			}
		}
		return array;
	}
}
