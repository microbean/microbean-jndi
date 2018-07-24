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

import java.net.URISyntaxException;
import java.net.URI;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

import javax.naming.spi.ObjectFactory;

public abstract class AbstractURLContextFactory implements ObjectFactory {

  protected final String scheme;
  
  protected AbstractURLContextFactory() {
    super();
    final String simpleClassName = this.getClass().getSimpleName();
    final int index = simpleClassName.lastIndexOf("URLContextFactory");
    this.scheme = simpleClassName.substring(0, index);
  }

  @Override
  public Object getObjectInstance(final Object object,
                                  final Name nameRelativeToContext, // nullable
                                  final Context contextContainingName, // nullable
                                  final Hashtable<?, ?> environment) // nullable
    throws Exception {

    final Object returnValue;
    
    if (object == null) {
      returnValue = this.newContext(environment, null);
    } else if (object instanceof String) {
      returnValue = this.newContext(environment, new URI(object.toString()));
    } else if (object instanceof String[]) {
      final String[] uriStrings = (String[])object;
      Context temp = null;
      for (final String uriString : uriStrings) {
        if (uriString != null) {
          try {
            final URI uri = new URI(uriString);
            temp = this.newContext(environment, uri);
            break;
          } catch (final NamingException | URISyntaxException neverMind) {

          }
        }
      }
      returnValue = temp;
    } else {
      returnValue = null;
    }

    return returnValue;
  }

  protected abstract Context newContext(final Hashtable<?, ?> environment, final URI uri) throws NamingException;
  
}
