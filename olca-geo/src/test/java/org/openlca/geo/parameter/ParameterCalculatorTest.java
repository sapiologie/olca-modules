package org.openlca.geo.parameter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openlca.geo.Tests;
import org.openlca.geo.kml.KmlFeature;
import org.openlca.geo.kml.KmlTests;

public class ParameterCalculatorTest {

	private DataStore dataStore;
	private IntersectionsCalculator intersectionsCalculator;
	private FeatureCalculator featureCalculator;

	@Before
	public void setUp() throws Exception {
		ShapeFileRepository repository = Tests.getRepository();
		dataStore = repository.openDataStore("states");
		intersectionsCalculator = new IntersectionsCalculator(dataStore);
		featureCalculator = new FeatureCalculator(dataStore);
	}

	@After
	public void tearDown() throws Exception {
		dataStore.dispose();
	}

	@Test
	public void testPoint() throws Exception {
		// a point in New Mexico; DRAWSEQ = 42
		KmlFeature feature = KmlTests.parse(Tests.getKml("point.kml"));
		Map<String, Double> shares = intersectionsCalculator.calculate(feature,
				Arrays.asList("DRAWSEQ"));
		Map<String, Double> params = featureCalculator
				.calculate(feature, Arrays.asList("DRAWSEQ"),
						new HashMap<String, Double>(), shares);
		Assert.assertTrue(params.size() == 1);
		Assert.assertEquals(42, params.get("DRAWSEQ"), 1e-17);
	}

	@Test
	public void testLine() throws Exception {
		// a line that crosses
		// New Mexico; DRAWSEQ = 42
		// Texas; DRAWSEQ = 41
		// Oklahoma; DRAWSEQ = 38
		// Kansas; DRAWSEQ = 34
		KmlFeature feature = KmlTests.parse(Tests.getKml("line.kml"));
		Map<String, Double> shares = intersectionsCalculator.calculate(feature,
				Arrays.asList("DRAWSEQ"));
		Map<String, Double> params = featureCalculator
				.calculate(feature, Arrays.asList("DRAWSEQ"),
						new HashMap<String, Double>(), shares);
		double val = params.get("DRAWSEQ");
		Assert.assertTrue(34 < val && val < 42);
	}

	@Test
	public void testPolygon() throws Exception {
		// a polygon that intersects
		// New Mexico; DRAWSEQ = 42
		// Texas; DRAWSEQ = 41
		// Oklahoma; DRAWSEQ = 38
		// Kansas; DRAWSEQ = 34
		// Colorado; DRAWSEQ = 32
		KmlFeature feature = KmlTests.parse(Tests.getKml("polygon.kml"));
		Map<String, Double> shares = intersectionsCalculator.calculate(feature,
				Arrays.asList("DRAWSEQ"));
		Map<String, Double> params = featureCalculator
				.calculate(feature, Arrays.asList("DRAWSEQ"),
						new HashMap<String, Double>(), shares);
		double val = params.get("DRAWSEQ");
		Assert.assertTrue(32 < val && val < 42);
	}

}
