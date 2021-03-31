package org.openlca.core.results.providers;

import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.MatrixData;
import org.openlca.core.matrix.solvers.JavaSolver;
import org.openlca.nativelib.NativeLib;
import org.openlca.nativelib.NativeSolver;

public final class ResultProviders {

	private ResultProviders() {
	}

	public static ResultProvider eagerOf(IDatabase db, MatrixData data) {
		var solver = NativeLib.isLoaded()
			? new NativeSolver()
			: new JavaSolver();
		if (data.hasLibraryLinks())
			return LazyLibraryProvider.of(db, data);

		var isSmall = data.techMatrix != null
									&& data.techMatrix.rows() < 3000;
		if (isSmall)
			return EagerResultProvider.create(data);
		return data.isSparse() && solver.hasSparseSupport()
			? LazyResultProvider.create(data)
			: EagerResultProvider.create(data);
	}

	public static ResultProvider lazyOf(IDatabase db, MatrixData data) {
		var solver = NativeLib.isLoaded()
			? new NativeSolver()
			: new JavaSolver();
		if (data.hasLibraryLinks())
			return LazyLibraryProvider.of(db, data);
		return data.isSparse() && solver.hasSparseSupport()
			? LazyResultProvider.create(data)
			: EagerResultProvider.create(data);
	}
}
