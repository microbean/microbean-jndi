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
package org.microbean.jndi.java;

import java.net.URI;

import java.util.Hashtable;
import java.util.Properties;

import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

import org.microbean.jndi.AbstractContext;
import org.microbean.jndi.AbstractURLContextFactory;
import org.microbean.jndi.Contexts;
import org.microbean.jndi.MapContext;
import org.microbean.jndi.ThreadSpecificContext;

public class javaURLContextFactory extends AbstractURLContextFactory {

  private static final Properties defaultSyntax = Contexts.leftToRightSlashSeparatedSyntax();
  
  public javaURLContextFactory() {
    super();
    assert "java".equals(this.scheme);
  }

  @Override
  protected Context newContext(final Hashtable<?, ?> environment, final URI uri) throws NamingException {
    if (uri != null && !this.scheme.equals(uri.getScheme())) {
      throw new NamingException("invalid URI: " + uri);
    }
    final NameParser nameParser = nameString -> new CompoundName(nameString, defaultSyntax);
    final Context returnValue = new MapContext(null, environment, nameParser, AbstractContext.EMPTY_NAME);
    final Name compName = new CompositeName("comp");
    final ThreadSpecificContext comp = new ThreadSpecificContext(null, environment, nameParser, compName);
    final Context env = comp.createSubcontext("env");
    assert env != null;
    returnValue.bind(compName, comp);
    return returnValue;
  }
  
}
