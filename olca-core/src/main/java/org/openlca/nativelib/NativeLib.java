package org.openlca.nativelib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.openlca.core.DataDir;
import org.openlca.julia.Julia;
import org.openlca.util.OS;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NativeLib contains utility functions for managing the native library
 * bindings.
 */
public class NativeLib {

	private enum LinkOption {
		NONE, BLAS, ALL
	}

	/**
	 * The version of the native interface that is used.
	 */
	public static final String VERSION = "2.0.0";

	private static final AtomicBoolean _loaded = new AtomicBoolean(false);
	private static final AtomicBoolean _withSparse = new AtomicBoolean(false);

	/**
	 * Returns true if the Julia libraries with openLCA bindings are loaded.
	 */
	public static boolean isLoaded() {
		return _loaded.get();
	}

	public static boolean hasSparseLibraries() {
		return _withSparse.get();
	}

	/**
	 * Downloads the native libraries of this version and saves them in the given
	 * folder. Does nothing, if that folder already contains all native libraries
	 * for the current platform.
	 */
	public static void downloadTo(File targetDir) {
		var skip = true;
		for (var libType : LibType.values()) {
			if (!libType.isPresent(targetDir)) {
				skip = false;
				break;
			}
		}
		if (skip)
			return;
		try {
			new LibraryDownload(targetDir).run();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the default location on the file system where our native libraries
	 * are located.
	 */
	public static File getDefaultDir() {
		var root = DataDir.root();
		var arch = System.getProperty("os.arch");
		var os = OS.get().toString();
		var path = Strings.join(
			List.of("native", VERSION, os, arch),
			File.separatorChar);
		return new File(root, path);
	}

	/**
	 * Tries to load the libraries from the default folder. Returns true if the
	 * libraries could be loaded or if they were already loaded.
	 */
	public static synchronized boolean load() {
		if (_loaded.get())
			return true;
		var log = LoggerFactory.getLogger(Julia.class);
		var dir = getDefaultDir();
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				log.error("Could not create library dir {}", dir);
				return false;
			}
		}

		// check if our base BLAS libraries are present and
		// extract them if necessary
		var blasLibs = libs(LinkOption.BLAS);

		for (var lib : blasLibs) {
			var libFile = new File(dir, lib);
			if (libFile.exists())
				continue;
			var arch = System.getProperty("os.arch");
			var jarPath = "/native/" + OS.get().toString()
				+ "/" + arch + "/" + lib;
			try {
				copyLib(jarPath, libFile);
			} catch (Exception e) {
				log.error("failed to extract library " + lib, e);
				return false;
			}
		}
		return loadFromDir(dir);
	}

	private static void copyLib(String jarPath, File file) throws IOException {
		var is = Julia.class.getResourceAsStream(jarPath);
		var os = new FileOutputStream(file);
		byte[] buf = new byte[1024];
		int len;
		while ((len = is.read(buf)) > 0) {
			os.write(buf, 0, len);
		}
		os.flush();
		os.close();
	}

	/**
	 * Loads the Julia libraries and openLCA bindings from the given folder. Returns
	 * true if the libraries could be loaded (at least there should be a `libjolca`
	 * library in the folder that could be loaded).
	 */
	public static boolean loadFromDir(File dir) {
		Logger log = LoggerFactory.getLogger(Julia.class);
		log.info("Try to load Julia libs and bindings from {}", dir);
		if (_loaded.get()) {
			log.info("Julia libs already loaded; do nothing");
			return true;
		}
		if (dir == null || !dir.exists() || !dir.isDirectory()) {
			log.warn("{} does not contain the Julia libraries", dir);
			return false;
		}
		synchronized (_loaded) {
			if (_loaded.get())
				return true;
			try {
				LinkOption opt = linkOption(dir);
				if (opt == null || opt == LinkOption.NONE) {
					log.info("No native libraries found");
					return false;
				}
				for (String lib : libs(opt)) {
					File f = new File(dir, lib);
					System.load(f.getAbsolutePath());
					log.info("loaded native library {}", f);
				}
				_loaded.set(true);
				if (opt == LinkOption.ALL) {
					_withSparse.set(true);
					log.info("Math libraries loaded with UMFPACK support.");
				} else {
					log.info("Math libraries loaded without UMFPACK support.");
				}
				return true;
			} catch (Error e) {
				log.error("Failed to load Julia libs from " + dir, e);
				return false;
			}
		}
	}

	private static String[] libs(LinkOption opt) {
		if (opt == null || opt == LinkOption.NONE)
			return null;

		OS os = OS.get();

		if (os == OS.WINDOWS) {
			if (opt == LinkOption.ALL) {
				return new String[]{
					"libsuitesparseconfig.dll",
					"libamd.dll",
					"libwinpthread-1.dll",
					"libgcc_s_seh-1.dll",
					"libquadmath-0.dll",
					"libgfortran-5.dll",
					"libopenblas64_.dll",
					"libcolamd.dll",
					"libcamd.dll",
					"libccolamd.dll",
					"libcholmod.dll",
					"libumfpack.dll",
					"olcar_withumf.dll",
				};
			} else {
				return new String[]{
					"libwinpthread-1.dll",
					"libgcc_s_seh-1.dll",
					"libquadmath-0.dll",
					"libgfortran-5.dll",
					"libopenblas64_.dll",
					"olcar.dll",
				};
			}
		}

		if (os == OS.LINUX) {
			if (opt == LinkOption.ALL) {
				return new String[]{
					"libgcc_s.so.1",
					"libsuitesparseconfig.so.5",
					"libccolamd.so.2",
					"libamd.so.2",
					"libcamd.so.2",
					"libcolamd.so.2",
					"libquadmath.so.0",
					"libgfortran.so.4",
					"libopenblas64_.so.0",
					"libcholmod.so.3",
					"libumfpack.so.5",
					"libolcar_withumf.so",
				};
			} else {
				return new String[]{
					"libgcc_s.so.1",
					"libquadmath.so.0",
					"libgfortran.so.4",
					"libopenblas64_.so.0",
					"libolcar.so",
				};
			}
		}

		if (os == OS.MAC) {
			if (opt == LinkOption.ALL) {
				return new String[]{
					"libgcc_s.1.dylib",
					"libquadmath.0.dylib",
					"libgfortran.5.dylib",
					"libopenblas64_.dylib",
					"libsuitesparseconfig.5.4.0.dylib",
					"libamd.2.4.6.dylib",
					"libccolamd.2.9.6.dylib",
					"libcolamd.2.9.6.dylib",
					"libcamd.2.4.6.dylib",
					"libcholmod.3.0.13.dylib",
					"libumfpack.5.7.8.dylib",
					"libolcar_withumf.dylib",
				};
			} else {
				return new String[]{
					"libgcc_s.1.dylib",
					"libquadmath.0.dylib",
					"libgfortran.5.dylib",
					"libopenblas64_.dylib",
					"libolcar.dylib",
				};
			}
		}
		return null;
	}

	/**
	 * Searches for the library which can be linked. When there are multiple link
	 * options it chooses the one with more functions.
	 */
	private static LinkOption linkOption(File dir) {
		if (dir == null || !dir.exists())
			return LinkOption.NONE;
		var files = dir.listFiles();
		if (files == null)
			return LinkOption.NONE;
		var opt = LinkOption.NONE;
		for (File f : files) {
			if (!f.isFile())
				continue;
			if (f.getName().contains("olcar_withumf")) {
				return LinkOption.ALL;
			}
			if (f.getName().contains("olcar")) {
				opt = LinkOption.BLAS;
			}
		}
		return opt;
	}
}
