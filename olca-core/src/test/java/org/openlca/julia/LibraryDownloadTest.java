package org.openlca.julia;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openlca.nativelib.NativeLib;

public class LibraryDownloadTest {

	/**
	 * This test is ignored as we only run it from time to time in order
	 * to check if our library downloads work. Before running the test,
	 * delete the `~./openLCA` folder. Otherwise you may do not really
	 * test something here.
	 */
	@Test
	@Ignore
	public void testFetchSparseLibs() {

		// first load the libraries from the jar
		Assert.assertTrue(NativeLib.load());
		Assert.assertTrue(NativeLib.isLoaded());
		Assert.assertFalse(NativeLib.hasSparseLibraries());

		// now fetch the sparse libraries from the web
		Assert.assertTrue(NativeLib.fetchSparseLibraries());
		Assert.assertTrue(NativeLib.isLoaded());
		Assert.assertTrue(NativeLib.hasSparseLibraries());
	}
}
