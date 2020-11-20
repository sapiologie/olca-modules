package org.openlca.core.database.references;

import java.io.Serializable;

import org.openlca.core.model.AbstractEntity;
import org.openlca.util.Strings;

/**
 * Describes an entity $e_r$ which is referenced from an entity $e_o$ where
 * $e_o$ is the owner of the reference and $e_r$ is the referenced entity. For
 * example, if a process has a reference to a location then the process takes
 * the role of $e_o$ and the location of $e_r$ in that reference.
 */
public class Reference implements Serializable {

	private static final long serialVersionUID = -3036634720068312246L;

	/**
	 * The owner of the reference, e.g. if a process has a reference to a
	 * location, the process is the owner.
	 */
	public long ownerId;

	/**
	 * The type of the owner of the references.
	 */
	public String ownerType;

	/**
	 * The ID of the referenced entity $e_r$
	 */
	public long id;

	/**
	 * The type of the referenced entity $e_r$.
	 */
	public String type;

	/**
	 * This is typically the name of the field in which the reference is stored
	 * in the owner $e_o$.
	 */
	public String property;

	/**
	 * The can be intermediate objects in a reference between an owner $e_o$ and
	 * a referenced entity $e_r$. For example in a reference process -> exchange
	 * -> flow, the exchange would be such an intermediate object. This field
	 * holds the ID of such an intermediate object in this case.
	 */
	public long nestedOwnerId;

	/**
	 * The type of the intermediate object, if present.
	 */
	public String nestedOwnerType;

	/**
	 * The field in which the intermediate object is stored in the owner $e_o$
	 * if present.
	 */
	public String nestedProperty;

	/**
	 * Indicates whether a reference is optional or not. In general a reference
	 * is optional if it does not break the calculation when it is missing or
	 * broken. For example, the unit of an exchange is not optional but the
	 * data set generator of a process is.
	 */
	public boolean optional;

	public Reference(String property, Class<? extends AbstractEntity> type, long id,
					 Class<? extends AbstractEntity> ownerType, long ownerId) {
		this(property, type, id, ownerType, ownerId, null, null, 0L, false);
	}

	public Reference(String property, Class<? extends AbstractEntity> type, long id,
					 Class<? extends AbstractEntity> ownerType, long ownerId,
					 boolean optional) {
		this(property, type, id, ownerType, ownerId, null, null, 0L, optional);
	}

	public Reference(String property, Class<? extends AbstractEntity> type, long id,
					 Class<? extends AbstractEntity> ownerType, long ownerId,
					 String nestedProperty, Class<? extends AbstractEntity> nestedOwnerType,
					 long nestedOwnerId, boolean optional) {
		this.property = property;
		this.type = type.getCanonicalName();
		this.id = id;
		this.ownerType = ownerType.getCanonicalName();
		this.ownerId = ownerId;
		this.nestedProperty = nestedProperty;
		this.nestedOwnerType = nestedOwnerType != null ? nestedOwnerType.getCanonicalName() : null;
		this.nestedOwnerId = nestedOwnerId;
		this.optional = optional;
	}

	private Reference () {
	}

	static Reference ownerID(long ownerID) {
		var ref = new Reference();
		ref.ownerId = ownerID;
		return ref;
	}

	Reference ownerType(Class<?> ownerType) {
		this.ownerType = ownerType.getCanonicalName();
		return this;
	}

	Reference property(String property) {
		this.property = property;
		return this;
	}

	Reference referencedID(long refID) {
		this.id = refID;
		return this;
	}

	Reference referencedType(Class<?> refType) {
		this.type = refType.getCanonicalName();
		return this;
	}

	Reference nestedOwnerID(long nestedID) {
		this.nestedOwnerId = nestedID;
		return this;
	}

	Reference nestedOwnerType(Class<?> nestedType) {
		this.nestedOwnerType = nestedType.getCanonicalName();
		return this;
	}

	Reference nestedProperty(String nestedProperty) {
		this.nestedProperty = nestedProperty;
		return this;
	}

	@SuppressWarnings("unchecked")
	public Class<? extends AbstractEntity> getType() {
		try {
			return (Class<? extends AbstractEntity>) Class.forName(type);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public Class<? extends AbstractEntity> getOwnerType() {
		try {
			return (Class<? extends AbstractEntity>) Class.forName(ownerType);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public Class<? extends AbstractEntity> getNestedOwnerType() {
		if (nestedOwnerType == null)
			return null;
		try {
			return (Class<? extends AbstractEntity>) Class.forName(nestedOwnerType);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Reference))
			return false;
		Reference other = (Reference) obj;
		if (other.id != id)
			return false;
		if (other.ownerId != ownerId)
			return false;
		if (other.nestedOwnerId != nestedOwnerId)
			return false;
		if (!Strings.nullOrEqual(other.property, property))
			return false;
		if (other.getType() != getType())
			return false;
		if (other.getOwnerType() != getOwnerType())
			return false;
		return other.getNestedOwnerType() == getNestedOwnerType();
	}

	@Override
	public int hashCode() {
		return (property + id + ownerId + nestedOwnerId + type + ownerType + nestedOwnerType).hashCode();
	}

}
