package org.openlca.core.database.references;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.model.Category;

class CategoryRefSearch implements IReferenceSearch {

	private final IDatabase db;

	CategoryRefSearch(IDatabase db) {
		this.db = db;
	}

	@Override
	public List<Reference> of(Set<Long> ids) {
		var refs = new ArrayList<Reference>();

		var handledRefs = new HashSet<Long>();

		var query = "select id, f_category from tbl_categories";
		NativeSql.on(db).query(query, r -> {
			var ownerID = r.getLong(1);
			if (!ids.contains(ownerID))
				return true;

			// f_category
			var refID = r.getLong(2);
			if (!handledRefs.contains(refID)) {
				handledRefs.add(refID);
				var ref = Reference.ownerID(ownerID)
						.ownerType(Category.class)
						.property("category")
						.referencedID(refID)
						.referencedType(Category.class);
				refs.add(ref);
			}

			return true;
		});

		return refs;
	}

}
