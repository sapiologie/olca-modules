package org.openlca.util;

import java.util.Locale;

/**
 * Constants for some operating systems which are retrieved from the system
 * property "os.name".
 */
public enum OS {

	LINUX("Linux"),

	MAC("macOS"),

	WINDOWS("Windows"),

	OTHER("Other");

	private final String name;

	OS(String name) {
		this.name = name;
	}

	private static OS detected = null;

	public static OS get() {
		if (detected != null)
			return detected;
		String os = System.getProperty("os.name", "generic")
				.toLowerCase(Locale.ENGLISH);
		if (os.contains("mac") || os.contains("darwin")) {
			detected = MAC;
		} else if (os.contains("windows")) {
			detected = WINDOWS;
		} else if (os.contains("linux")) {
			detected = LINUX;
		} else {
			detected = OTHER;
		}
		return detected;
	}

	@Override
	public String toString() {
		return name;
	}

}
