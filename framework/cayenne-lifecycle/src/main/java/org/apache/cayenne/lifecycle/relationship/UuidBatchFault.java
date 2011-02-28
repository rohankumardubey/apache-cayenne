/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.lifecycle.relationship;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.lifecycle.uuid.UuidCoder;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.SelectQuery;

/**
 * Provides lazy faulting functionality for a map of objects identified by UUID.
 * 
 * @since 3.1
 */
class UuidBatchFault {

    private ObjectContext context;
    private List<UuidBatchSourceItem> sources;
    private volatile Map<String, Object> resolved;

    UuidBatchFault(ObjectContext context, List<UuidBatchSourceItem> sources) {
        this.context = context;
        this.sources = sources;
    }

    Map<String, Object> getObjects() {

        if (resolved == null) {

            synchronized (this) {

                if (resolved == null) {
                    resolved = fetchObjects();
                }
            }
        }

        return resolved;
    }

    private Map<String, Object> fetchObjects() {

        if (sources == null) {
            return Collections.EMPTY_MAP;
        }

        EntityResolver resolver = context.getEntityResolver();

        // simple case of one query, handle it separately for performance reasons
        if (sources.size() == 1) {

            String uuid = sources.iterator().next().getUuid();
            String entityName = UuidCoder.getEntityName(uuid);

            ObjEntity entity = resolver.getObjEntity(entityName);
            ObjectId id = new UuidCoder(entity).toObjectId(uuid);

            Object object = Cayenne.objectForQuery(context, new ObjectIdQuery(id));
            if (object == null) {
                return Collections.EMPTY_MAP;
            }
            else {
                return Collections.singletonMap(uuid, object);
            }
        }

        Map<String, SelectQuery> queriesByEntity = new HashMap<String, SelectQuery>();
        Map<String, UuidCoder> codersByEntity = new HashMap<String, UuidCoder>();

        for (UuidBatchSourceItem source : sources) {

            String uuid = source.getUuid();
            String entityName = UuidCoder.getEntityName(uuid);
            UuidCoder coder = codersByEntity.get(entityName);
            SelectQuery query;

            if (coder == null) {
                coder = new UuidCoder(resolver.getObjEntity(entityName));
                codersByEntity.put(entityName, coder);

                query = new SelectQuery(entityName);
                queriesByEntity.put(entityName, query);
            }
            else {
                query = queriesByEntity.get(entityName);
            }

            ObjectId id = coder.toObjectId(uuid);
            Expression idExp = ExpressionFactory.matchAllDbExp(
                    id.getIdSnapshot(),
                    Expression.EQUAL_TO);
            query.orQualifier(idExp);
        }

        int capacity = (int) Math.ceil(sources.size() / 0.75d);
        Map<String, Object> results = new HashMap<String, Object>(capacity);

        for (SelectQuery query : queriesByEntity.values()) {

            UuidCoder coder = codersByEntity.get(query.getRoot());
            List<DataObject> objects = context.performQuery(query);
            for (DataObject object : objects) {
                String uuid = coder.toUuid(object.getObjectId());
                results.put(uuid, object);
            }
        }

        return results;
    }
}
