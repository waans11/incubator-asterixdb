/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.asterix.common.ioopcallbacks;

import java.util.List;

import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.data.std.primitive.LongPointable;
import org.apache.hyracks.storage.am.common.api.IMetadataPageManager;
import org.apache.hyracks.storage.am.common.api.ITreeIndex;
import org.apache.hyracks.storage.am.common.freepage.MutableArrayValueReference;
import org.apache.hyracks.storage.am.lsm.common.api.ILSMComponent;
import org.apache.hyracks.storage.am.lsm.common.api.ILSMDiskComponent;
import org.apache.hyracks.storage.am.lsm.common.api.ILSMIOOperationCallback;
import org.apache.hyracks.storage.am.lsm.common.api.LSMOperationType;

// A single LSMIOOperationCallback per LSM index used to perform actions around Flush and Merge operations
public abstract class AbstractLSMIOOperationCallback implements ILSMIOOperationCallback {
    public static final MutableArrayValueReference LSN_KEY = new MutableArrayValueReference("LSN".getBytes());
    public static final long INVALID = -1L;

    // First LSN per mutable component
    protected long[] firstLSNs;
    // A boolean array to keep track of flush operations
    protected boolean[] flushRequested;
    // I think this was meant to be mutableLastLSNs
    // protected long[] immutableLastLSNs;
    protected long[] mutableLastLSNs;
    // Index of the currently flushing or next to be flushed component
    protected int readIndex;
    // Index of the currently being written to component
    protected int writeIndex;

    @Override
    public void setNumOfMutableComponents(int count) {
        mutableLastLSNs = new long[count];
        firstLSNs = new long[count];
        flushRequested = new boolean[count];
        readIndex = 0;
        writeIndex = 0;
    }

    @Override
    public void beforeOperation(LSMOperationType opType) {
        if (opType == LSMOperationType.FLUSH) {
            /*
             * This method was called on the scheduleFlush operation.
             * We set the lastLSN to the last LSN for the index (the LSN for the flush log)
             * We mark the component flushing flag
             * We then move the write pointer to the next component and sets its first LSN to the flush log LSN
             */
            synchronized (this) {
                flushRequested[writeIndex] = true;
                writeIndex = (writeIndex + 1) % mutableLastLSNs.length;
                // Set the firstLSN of the next component unless it is being flushed
                if (writeIndex != readIndex) {
                    firstLSNs[writeIndex] = mutableLastLSNs[writeIndex];
                }
            }
        }
    }

    @Override
    public void afterFinalize(LSMOperationType opType, ILSMDiskComponent newComponent) {
        // The operation was complete and the next I/O operation for the LSM index didn't start yet
        if (opType == LSMOperationType.FLUSH && newComponent != null) {
            synchronized (this) {
                flushRequested[readIndex] = false;
                // if the component which just finished flushing is the component that will be modified next,
                // we set its first LSN to its previous LSN
                if (readIndex == writeIndex) {
                    firstLSNs[writeIndex] = mutableLastLSNs[writeIndex];
                }
                readIndex = (readIndex + 1) % mutableLastLSNs.length;
            }
        }
    }

    public abstract long getComponentLSN(List<? extends ILSMComponent> oldComponents) throws HyracksDataException;

    public void putLSNIntoMetadata(ILSMDiskComponent index, List<ILSMComponent> oldComponents)
            throws HyracksDataException {
        index.getMetadata().put(LSN_KEY, LongPointable.FACTORY.createPointable(getComponentLSN(oldComponents)));
    }

    public static long getTreeIndexLSN(ITreeIndex treeIndex) throws HyracksDataException {
        LongPointable pointable = new LongPointable();
        IMetadataPageManager metadataPageManager = (IMetadataPageManager) treeIndex.getPageManager();
        metadataPageManager.get(metadataPageManager.createMetadataFrame(), LSN_KEY, pointable);
        return pointable.getLength() == 0 ? INVALID : pointable.longValue();
    }

    public void updateLastLSN(long lastLSN) {
        mutableLastLSNs[writeIndex] = lastLSN;
    }

    public void setFirstLSN(long firstLSN) {
        // We make sure that this method is only called on an empty component so the first LSN is not set incorrectly
        firstLSNs[writeIndex] = firstLSN;
    }

    public synchronized long getFirstLSN() {
        // We make sure that this method is only called on a non-empty component so the returned LSN is meaningful
        // The firstLSN is always the lsn of the currently being flushed component or the next
        // to be flushed when no flush operation is on going
        return firstLSNs[readIndex];
    }

    public synchronized boolean hasPendingFlush() {

        for (int i = 0; i < flushRequested.length; i++) {
            if (flushRequested[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param component
     * @param componentFilePath
     * @return The LSN byte offset in the LSM disk component if the index is valid,
     *         otherwise {@link IMetadataPageManager#INVALID_LSN_OFFSET}.
     * @throws HyracksDataException
     */
    public abstract long getComponentFileLSNOffset(ILSMDiskComponent component, String componentFilePath)
            throws HyracksDataException;
}
