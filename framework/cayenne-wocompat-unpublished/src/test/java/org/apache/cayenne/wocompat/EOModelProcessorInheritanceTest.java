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

package org.apache.cayenne.wocompat;

import java.net.URL;

import junit.framework.TestCase;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

public class EOModelProcessorInheritanceTest extends TestCase {

    private URL url;
    protected EOModelProcessor processor;

    @Override
    public void setUp() throws Exception {
        processor = new EOModelProcessor();

        url = getClass().getClassLoader().getResource("inheritance.eomodeld/");
        assertNotNull(url);
    }

    public void testLoadAbstractEntity() throws Exception {
        DataMap map = processor.loadEOModel(url);

        ObjEntity abstractE = map.getObjEntity("AbstractEntity");
        assertNotNull(abstractE);
        assertNull(abstractE.getDbEntityName());
        assertEquals("AbstractEntityClass", abstractE.getClassName());
    }

    public void testLoadConcreteEntity() throws Exception {
        DataMap map = processor.loadEOModel(url);

        ObjEntity concreteE = map.getObjEntity("ConcreteEntityOne");
        assertNotNull(concreteE);
        assertEquals("AbstractEntity", concreteE.getSuperEntityName());

        assertEquals("CONCRETE_ENTITY_ONE", concreteE.getDbEntityName());
        assertEquals("ConcreteEntityClass", concreteE.getClassName());
        assertEquals("AbstractEntityClass", concreteE.getSuperClassName());
    }

    public void testLoadFlattenedRelationship() throws Exception {
        DataMap map = processor.loadEOModel(url);

        ObjEntity e1 = map.getObjEntity("HelperFlatEntity");
        assertNotNull(e1);

        ObjRelationship fr = (ObjRelationship) e1.getRelationship("singleTables");
        assertNotNull(fr);
        assertEquals("singleTableJoins.toSingleTable", fr.getDbRelationshipPath());
        assertEquals("SingleTableConcreteEntityOne", fr.getTargetEntityName());
    }
}
