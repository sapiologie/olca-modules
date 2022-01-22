package org.openlca.jsonld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.core.model.ModelType;

/**
 * A simple implementation of the {@link JsonStoreReader} and
 * {@link JsonStoreWriter} interfaces that stores data in memory.
 */
public class MemStore implements JsonStoreReader, JsonStoreWriter {

	private final Map<String, JsonElement> jsonData = new HashMap<>();
	private final Map<String, byte[]> byteData = new HashMap<>();

	@Override
	public List<String> getRefIds(ModelType type) {
		var prefix = ModelPath.folderOf(type) + '/';
		var ids = new ArrayList<String>();
		for (var path : jsonData.keySet()) {
			if (!path.startsWith(prefix) || !path.endsWith(".json"))
				continue;
			var id = path.substring(prefix.length(), path.length() - 5);
			ids.add(id);
		}
		return ids;
	}

	@Override
	public List<String> getBinFiles(ModelType type, String refId) {
		var prefix = ModelPath.binFolderOf(type, refId) + '/';
		return byteData.keySet().stream()
			.filter(p -> p.startsWith(prefix))
			.toList();
	}

	@Override
	public JsonElement getJson(String path) {
		return jsonData.get(path);
	}

	@Override
	public byte[] getBytes(String path) {
		return byteData.get(path);
	}

	@Override
	public void put(ModelType type, JsonObject obj) {
		if (type == null || obj == null)
			return;
		var refId = Json.getString(obj, "@id");
		var path = ModelPath.jsonOf(type, refId);
		jsonData.put(path, obj);
	}

	@Override
	public void put(String path, byte[] bytes) {
		byteData.put(path, bytes);
	}
}
