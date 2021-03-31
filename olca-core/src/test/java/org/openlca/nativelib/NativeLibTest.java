package org.openlca.nativelib;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;
import org.openlca.util.Dirs;

public class NativeLibTest {

	@Test
	public void testDownload() throws IOException {
		var tempDir = Files.createTempDirectory("_olca_").toFile();
		NativeLib.downloadTo(tempDir);
		for (var type : LibType.values()) {
			assertTrue(type.isPresent(tempDir));
		}
		// System.out.println(tempDir.getAbsolutePath());
		Dirs.delete(tempDir);
	}
}
