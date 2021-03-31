package org.openlca.nativelib;

import org.openlca.julia.Julia;

public class UmfFactorizedMatrix {
    final long pointer;

    UmfFactorizedMatrix(long pointer) {
        this.pointer = pointer;
    }

    public void dispose() {
        Julia.umfDispose(pointer);
    }

}
