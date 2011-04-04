
    kernel void VectorAddGM(global const int* a, global const int* b, global int* c, int iNumElements) {
        int iGID = get_global_id(0);
        if (iGID >= iNumElements)  {
            return;
        }
        c[iGID] = a[iGID] + b[iGID];
    }

    kernel void Test(global const int* a, global const int* b, global int* c, int iNumElements) {
        int iGID = get_global_id(0);
        if (iGID >= iNumElements)  {
            return;
        }
        c[iGID] = iGID;
    }

    kernel void add(global int* a, int value, int iNumElements) {
        int iGID = get_global_id(0);
        if (iGID >= iNumElements)  {
            return;
        }
        a[iGID] += value;
    }

    kernel void mul(global int* a, int value, int iNumElements) {

        int iGID = get_global_id(0);
        if (iGID >= iNumElements)  {
            return;
        }
        a[iGID] *= value;
    }
