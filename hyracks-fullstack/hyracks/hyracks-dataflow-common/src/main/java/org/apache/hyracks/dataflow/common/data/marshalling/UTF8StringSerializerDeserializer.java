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
package org.apache.hyracks.dataflow.common.data.marshalling;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hyracks.api.dataflow.value.ISerializerDeserializer;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.util.string.UTF8StringReader;
import org.apache.hyracks.util.string.UTF8StringUtil;
import org.apache.hyracks.util.string.UTF8StringWriter;

public class UTF8StringSerializerDeserializer implements ISerializerDeserializer<String> {

    private static final long serialVersionUID = 1L;
    private final UTF8StringWriter utf8StringWriter;
    private final UTF8StringReader utf8StringReader;

    public UTF8StringSerializerDeserializer() {
        this.utf8StringWriter = null;
        this.utf8StringReader = null;
    }

    public UTF8StringSerializerDeserializer(UTF8StringWriter utf8StringWriter, UTF8StringReader utf8StringReader) {
        this.utf8StringWriter = utf8StringWriter;
        this.utf8StringReader = utf8StringReader;
    }

    @Override
    public String deserialize(DataInput in) throws HyracksDataException {
        try {
            return UTF8StringUtil.readUTF8(in, utf8StringReader);
        } catch (IOException e) {
            throw new HyracksDataException(e);
        }
    }

    @Override
    public void serialize(String instance, DataOutput out) throws HyracksDataException {
        try {
            UTF8StringUtil.writeUTF8(instance, out, utf8StringWriter);
        } catch (IOException e) {
            throw new HyracksDataException(e);
        }
    }
}
