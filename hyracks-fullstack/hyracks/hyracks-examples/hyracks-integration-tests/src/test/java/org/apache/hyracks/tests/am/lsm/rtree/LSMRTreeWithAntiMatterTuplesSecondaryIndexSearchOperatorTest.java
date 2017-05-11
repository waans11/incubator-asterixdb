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

package org.apache.hyracks.tests.am.lsm.rtree;

import java.io.DataOutput;

import org.apache.hyracks.api.constraints.PartitionConstraintHelper;
import org.apache.hyracks.api.dataflow.IOperatorDescriptor;
import org.apache.hyracks.api.dataflow.value.IBinaryComparatorFactory;
import org.apache.hyracks.api.dataflow.value.ILinearizeComparatorFactory;
import org.apache.hyracks.api.dataflow.value.ISerializerDeserializer;
import org.apache.hyracks.api.dataflow.value.RecordDescriptor;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.api.io.FileSplit;
import org.apache.hyracks.api.job.JobSpecification;
import org.apache.hyracks.dataflow.common.comm.io.ArrayTupleBuilder;
import org.apache.hyracks.dataflow.common.data.marshalling.DoubleSerializerDeserializer;
import org.apache.hyracks.dataflow.std.connectors.OneToOneConnectorDescriptor;
import org.apache.hyracks.dataflow.std.file.ConstantFileSplitProvider;
import org.apache.hyracks.dataflow.std.file.IFileSplitProvider;
import org.apache.hyracks.dataflow.std.file.PlainFileWriterOperatorDescriptor;
import org.apache.hyracks.dataflow.std.misc.ConstantTupleSourceOperatorDescriptor;
import org.apache.hyracks.storage.am.common.api.IMetadataPageManagerFactory;
import org.apache.hyracks.storage.am.common.api.IPrimitiveValueProviderFactory;
import org.apache.hyracks.storage.am.common.impls.NoOpOperationCallbackFactory;
import org.apache.hyracks.storage.am.rtree.dataflow.RTreeSearchOperatorDescriptor;
import org.apache.hyracks.storage.am.rtree.frames.RTreePolicyType;
import org.apache.hyracks.storage.common.IResourceFactory;
import org.apache.hyracks.test.support.TestStorageManagerComponentHolder;
import org.apache.hyracks.tests.am.common.ITreeIndexOperatorTestHelper;
import org.apache.hyracks.tests.am.rtree.RTreeSecondaryIndexSearchOperatorTest;
import org.junit.Test;

public class LSMRTreeWithAntiMatterTuplesSecondaryIndexSearchOperatorTest
        extends RTreeSecondaryIndexSearchOperatorTest {
    public LSMRTreeWithAntiMatterTuplesSecondaryIndexSearchOperatorTest() {
        this.rTreeType = RTreeType.LSMRTREE_WITH_ANTIMATTER;
    }

    @Override
    protected ITreeIndexOperatorTestHelper createTestHelper() throws HyracksDataException {
        return new LSMRTreeWithAntiMatterTuplesOperatorTestHelper(TestStorageManagerComponentHolder.getIOManager());
    }

    @Override
    protected IResourceFactory createSecondaryResourceFactory(
            IPrimitiveValueProviderFactory[] secondaryValueProviderFactories, RTreePolicyType rtreePolicyType,
            IBinaryComparatorFactory[] btreeComparatorFactories, ILinearizeComparatorFactory linearizerCmpFactory,
            int[] btreeFields) {
        return ((LSMRTreeWithAntiMatterTuplesOperatorTestHelper) testHelper).getSecondaryLocalResourceFactory(
                storageManager, secondaryValueProviderFactories, rtreePolicyType, btreeComparatorFactories,
                linearizerCmpFactory, btreeFields, secondaryTypeTraits, secondaryComparatorFactories,
                (IMetadataPageManagerFactory) pageManagerFactory);
    }

    @Test
    public void shouldWriteFilterValueIfAppendFilterIsTrue() throws Exception {
        JobSpecification spec = new JobSpecification();
        // build tuple
        ArrayTupleBuilder tb = new ArrayTupleBuilder(secondaryKeyFieldCount);
        DataOutput dos = tb.getDataOutput();
        tb.reset();
        DoubleSerializerDeserializer.INSTANCE.serialize(61.2894, dos);
        tb.addFieldEndOffset();
        DoubleSerializerDeserializer.INSTANCE.serialize(-149.624, dos);
        tb.addFieldEndOffset();
        DoubleSerializerDeserializer.INSTANCE.serialize(61.8894, dos);
        tb.addFieldEndOffset();
        DoubleSerializerDeserializer.INSTANCE.serialize(-149.024, dos);
        tb.addFieldEndOffset();
        ISerializerDeserializer[] keyRecDescSers =
                { DoubleSerializerDeserializer.INSTANCE, DoubleSerializerDeserializer.INSTANCE,
                        DoubleSerializerDeserializer.INSTANCE, DoubleSerializerDeserializer.INSTANCE };
        RecordDescriptor keyRecDesc = new RecordDescriptor(keyRecDescSers);
        ConstantTupleSourceOperatorDescriptor keyProviderOp = new ConstantTupleSourceOperatorDescriptor(spec,
                keyRecDesc, tb.getFieldEndOffsets(), tb.getByteArray(), tb.getSize());
        PartitionConstraintHelper.addAbsoluteLocationConstraint(spec, keyProviderOp, NC1_ID);
        int[] keyFields = { 0, 1, 2, 3 };
        RTreeSearchOperatorDescriptor secondarySearchOp = new RTreeSearchOperatorDescriptor(spec,
                secondaryWithFilterRecDesc, keyFields, true, true, secondaryHelperFactory, false, false, null,
                NoOpOperationCallbackFactory.INSTANCE, null, null, false);
        PartitionConstraintHelper.addAbsoluteLocationConstraint(spec, secondarySearchOp, NC1_ID);

        IFileSplitProvider outSplits = new ConstantFileSplitProvider(new FileSplit[] { createFile(nc1) });
        IOperatorDescriptor printer = new PlainFileWriterOperatorDescriptor(spec, outSplits, ",");
        PartitionConstraintHelper.addAbsoluteLocationConstraint(spec, printer, NC1_ID);
        spec.connect(new OneToOneConnectorDescriptor(spec), keyProviderOp, 0, secondarySearchOp, 0);
        spec.connect(new OneToOneConnectorDescriptor(spec), secondarySearchOp, 0, printer, 0);
        spec.addRoot(printer);
        runTest(spec);
    }
}
