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

import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.NamingException;

import org.microbean.jndi.AbstractURLContextFactory;
import org.microbean.jndi.Contexts;
import org.microbean.jndi.MapContext;

public class javaURLContextFactory extends AbstractURLContextFactory {

  private static final Properties defaultSyntax = Contexts.leftToRightSlashSeparatedSyntax();
  
  public javaURLContextFactory() {
    super();
    assert "java".equals(this.scheme);
  }

  @Override
  protected Context newContext(final Hashtable<?, ?> environment, final URI uri) throws NamingException {
    if (uri != null && !this.scheme.equals(uri.getScheme())) {
      throw new NamingException("invalid uri: " + uri);
    }
    final Context returnValue = new MapContext(environment, nameString -> new CompoundName(nameString, defaultSyntax));
    final Context comp = returnValue.createSubcontext("comp");
    assert comp != null;
    final Context env = comp.createSubcontext("env");
    assert env != null;
    return returnValue;
  }
  
}
