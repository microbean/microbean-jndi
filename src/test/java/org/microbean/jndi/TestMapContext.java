/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2018 microBean.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.jndi;

import java.util.Enumeration;
import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestMapContext {  

  private static final Properties syntax = Contexts.leftToRightSlashSeparatedSyntax();

  private Context context;
  
  public TestMapContext() {
    super();
  }

  @Before
  public void setUp() throws NamingException {
    this.tearDown();
    this.context = new MapContext(null, null, name -> new CompoundName(name, syntax), null);
  }

  @After
  public void tearDown() throws NamingException {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  public void testCreateSubcontext() throws NamingException {
    this.context.createSubcontext("a");
  }
  
  @Test
  public void testLookupOfJavaColon() throws NamingException {
    final Object result = this.context.lookup("java:");
    assertTrue(result instanceof Context);
    final Context subcontext = (Context)result;
    try {
      final Enumeration<?> enumeration = subcontext.list("");
      assertNotNull(enumeration);
      assertFalse(enumeration.hasMoreElements());
    } finally {
      subcontext.close();
    }
  }

  @Test
  public void testOneLevelBindAndVariousEquivalentLookups() throws NamingException {
    this.context.bind("TransactionManager", "foobar");
    Object result = this.context.lookup("java:/TransactionManager");
    assertEquals("foobar", result);
    result = this.context.lookup("TransactionManager");
    assertEquals("foobar", result);
    result = this.context.lookup("java://///TransactionManager");
  }
  
}
