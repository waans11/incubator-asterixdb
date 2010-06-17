/*
 * Copyright 2009-2010 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uci.ics.hyracks.api.dataflow;

import java.io.Serializable;
import java.util.UUID;

public final class ActivityNodeId implements Serializable {
    private static final long serialVersionUID = 1L;
    private final OperatorDescriptorId odId;
    private final UUID id;

    public ActivityNodeId(OperatorDescriptorId odId, UUID id) {
        this.odId = odId;
        this.id = id;
    }

    public OperatorDescriptorId getOperatorDescriptorId() {
        return odId;
    }

    public UUID getLocalId() {
        return id;
    }

    @Override
    public int hashCode() {
        return odId.hashCode() + id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActivityNodeId)) {
            return false;
        }
        ActivityNodeId other = (ActivityNodeId) o;
        return other.odId.equals(odId) && other.id.equals(id);
    }

    public String toString() {
        return "ANID:" + id;
    }
}