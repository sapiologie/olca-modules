package org.openlca.core.database;

import java.util.HashMap;

class CategoryPath {

	private final HashMap<Long, Long> parents = new HashMap<>();
	private final HashMap<Long, String> names = new HashMap<>();
	private final HashMap<Long, String> cache = new HashMap<>();

	CategoryPath(IDatabase db) {
		String sql = "select id, name, f_category from tbl_categories";
		try {
			NativeSql.on(db).query(sql, r -> {
				long id = r.getLong(1);
				names.put(id, r.getString(2));
				long parent = r.getLong(3);
				if (!r.wasNull()) {
					parents.put(id, parent);
				}
				return true;
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	String get(long id) {
		String cached = cache.get(id);
		if (cached != null)
			return cached;
		String path = null;
		long pid = id;
		while (true) {
			String name = names.get(pid);
			if (name == null)
				break;
			if (path == null) {
				path = name;
			} else {
				path = name + "/" + path;
			}
			Long parent = parents.get(pid);
			if (parent == null)
				break;
			pid = parent;
		}
		cache.put(id, path);
		return path;
	}
}
