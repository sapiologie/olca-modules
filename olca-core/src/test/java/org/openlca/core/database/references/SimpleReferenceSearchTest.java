package org.openlca.core.database.references;

import org.junit.Test;
import org.openlca.core.Tests;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Process;
import org.openlca.core.model.UnitGroup;

public class SimpleReferenceSearchTest {

	@Test
	public void test() {

		var units = UnitGroup.of("Units of mass", "kg");
		var mass = FlowProperty.of("Mass", units);
		var co2 = Flow.elementary("CO2", mass);
		var steel = Flow.product("Steel", mass);
		var process = Process.of("Steel production", steel);
		process.output(co2, 2);
		var impact = ImpactCategory.of("GWP", "CO2 eq.");
		impact.factor(co2, 1);
		var method = ImpactMethod.of("Method");
		method.impactCategories.add(impact);

		var db = Tests.getDb();
		db.clear();
		db.insert(
				units,
				mass,
				co2,
				steel,
				process,
				impact,
				method);

		var refs = References.of(db, process);
		for (var ref : refs) {
			System.out.println(ref);
		}
	}

}
