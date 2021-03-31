package org.openlca.nativelib;

import java.io.File;

import org.openlca.util.OS;

public enum LibType {

	BLAS() {
		@Override
		String[] files() {
			return switch (OS.get()) {

				case WINDOWS -> new String[]{
					"libwinpthread-1.dll",
					"libgcc_s_seh-1.dll",
					"libquadmath-0.dll",
					"libgfortran-5.dll",
					"libopenblas64_.dll",
					"olcar.dll",
				};

				case LINUX -> new String[]{
					"libgcc_s.so.1",
					"libquadmath.so.0",
					"libgfortran.so.4",
					"libopenblas64_.so.0",
					"libolcar.so",
				};

				case MAC -> new String[]{
					"libgcc_s.1.dylib",
					"libquadmath.0.dylib",
					"libgfortran.5.dylib",
					"libopenblas64_.dylib",
					"libolcar.dylib",
				};

				case OTHER -> new String[0];
			};
		}
	},

	UMFPACK() {
		@Override
		String[] files() {
			return switch (OS.get()) {

				case WINDOWS -> new String[]{
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

				case LINUX -> new String[] {
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

				case MAC -> new String[] {
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

				case OTHER -> new String[0];
			};
		}
	};

	/**
	 * Get the names of native library files for the respective
	 * library type on the current platform. The library names
	 * must be provided in the order in which the need to be
	 * loaded.
	 */
	abstract String[] files();

	/**
	 * Returns true if the given folder contains all native
	 * library files of this library type.
	 */
	boolean isPresent(File dir) {
		if (dir == null || !dir.exists())
			return false;
		for (var f : files()) {
			var file = new File(dir, f);
			if (!file.exists())
				return false;
		}
		return true;
	}

}
