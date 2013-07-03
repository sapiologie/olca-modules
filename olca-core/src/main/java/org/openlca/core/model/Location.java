package org.openlca.core.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "tbl_locations")
public class Location extends RootEntity {

	@Column(name = "code")
	private String code;

	@Lob
	@Column(name = "description")
	private String description;

	@Column(name = "latitude")
	private double latitude;

	@Column(name = "longitude")
	private double longitude;

	@Column(name = "name")
	private String name;

	public String getCode() {
		return code;
	}

	@Override
	public Location clone() {
		Location clone = new Location();
		clone.setCode(getCode());
		clone.setDescription(getDescription());
		clone.setLatitude(getLatitude());
		clone.setLongitude(getLongitude());
		clone.setName(getName());
		clone.setRefId(UUID.randomUUID().toString());
		return clone;
	}

	public String getDescription() {
		return description;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

}
