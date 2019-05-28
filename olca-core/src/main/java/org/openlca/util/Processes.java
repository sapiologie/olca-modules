package org.openlca.util;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class Processes {

	private Processes() {
	}

	/**
	 * Searches the given database for a process with the given label. A label
	 * can contain a suffix with a location code which is considered in this
	 * function.
	 */
	public static ProcessDescriptor findForLabel(IDatabase db, String label) {
		if (db == null || label == null)
			return null;
		String fullName = label;
		String name = null;
		String location = null;
		if (fullName.contains(" - ")) {
			int splitIdx = fullName.lastIndexOf(" - ");
			name = fullName.substring(0, splitIdx).trim();
			location = fullName.substring(splitIdx + 3).trim();
		}

		ProcessDescriptor selected = null;
		ProcessDao pDao = new ProcessDao(db);
		for (ProcessDescriptor d : pDao.getDescriptors()) {
			if (!Strings.nullOrEqual(fullName, d.name)
					&& !Strings.nullOrEqual(name, d.name))
				continue;
			if (selected == null) {
				selected = d;
				if (matchLocation(selected, location))
					break;
				else
					continue;
			}
			if (matchLocation(d, location)
					&& !matchLocation(selected, location)) {
				selected = d;
				break;
			}
		}
		return selected;
	}

	private static boolean matchLocation(ProcessDescriptor d, String code) {
		if (d == null)
			return false;
		return Strings.nullOrEqual(d.location, code);
	}
}
