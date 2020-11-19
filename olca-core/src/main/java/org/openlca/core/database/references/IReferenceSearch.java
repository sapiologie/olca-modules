package org.openlca.core.database.references;

import java.util.List;
import java.util.Set;

import org.openlca.core.model.descriptors.CategorizedDescriptor;

/**
 * A reference search is the inverse of a usage search: while a usage search
 * searches in which other entities an entity is used, a reference search searches
 * which other entities are referenced from an entity. Such references may point
 * to non-existing entities (when something is not correct with the database) and
 * it is one goal of the reference search to find such cases.
 */
public interface IReferenceSearch {

	/**
	 * Find the references where the entities with the given IDs
	 * are the reference owner.
	 */
	List<Reference> of(Set<Long> ids);

}
