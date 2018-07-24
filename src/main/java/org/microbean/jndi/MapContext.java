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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;

import javax.naming.spi.NamingManager;

public class MapContext extends AbstractContext {

  private static final Pattern urlPattern = Pattern.compile("^[a-zA-Z]+?:(.*)$");
  
  private final Map<String, Object> map;

  public MapContext() {
    this(null, null, null, null);
  }
  
  public MapContext(final Hashtable<?, ?> environment, final NameParser nameParser) {
    this(null, environment, nameParser, null);
  }
  
  public MapContext(final Map<? extends String, ?> map, final Hashtable<?, ?> environment, final NameParser nameParser, final Name prefix) {
    super(environment, nameParser, prefix);
    if (map == null || map.isEmpty()) {
      this.map = new HashMap<>();
    } else {
      this.map = new HashMap<>(map);
    }
  }

  @Override
  protected Context copy() {
    return new MapContext(this.map, this.environment, this.nameParser, this.prefix);
  }

  protected final Name toCompoundName(final Name name) throws NamingException {
    Objects.requireNonNull(name);
    final Name returnValue;
    if (name instanceof CompositeName) {
      returnValue = this.toCompoundName((CompositeName)name);
    } else {
      returnValue = name;
    }
    return returnValue;
  }

  protected Name toCompoundName(final CompositeName compositeName) throws NamingException {
    Objects.requireNonNull(compositeName);
    final Name returnValue = this.nameParser.parse("");
    assert returnValue != null;
    assert returnValue.size() == 0;
    assert returnValue.isEmpty();
    final int size = compositeName.size();
    assert size >= 0;
    if (size > 0) {
      int i = 0;
      for (; i < size; i++) {
        final String compositeNameComponent = compositeName.get(i);
        assert compositeNameComponent != null;
        if (!compositeNameComponent.isEmpty()) {
          String compositeNameNonUrlComponent = compositeNameComponent;
          if (i == 0) {
            final Matcher urlMatcher = urlPattern.matcher(compositeNameComponent);
            assert urlMatcher != null;
            if (urlMatcher.matches()) {
              compositeNameNonUrlComponent = urlMatcher.group(1);
            }
          }
          assert compositeNameNonUrlComponent != null;
          if (!compositeNameNonUrlComponent.isEmpty()) {
            try {
              returnValue.addAll(this.nameParser.parse(compositeNameNonUrlComponent));
            } catch (final InvalidNameException parsingFailed) {
              break;
            }
          }
        }
      }

      if (i < size) {
        // at this point i identifies the suffix to the CannotProceedException
        // TODO: deal with federation
        throw new InvalidNameException("compositeName could not be entirely parsed; successful parse: " + returnValue + "; remaining: " + compositeName.getSuffix(i));
      }
    }
    return returnValue;
  }

  private final Object get(final String mapKey) throws NamingException {
    Objects.requireNonNull(mapKey);
    return this.get(mapKey, this.nameParser.parse(mapKey));
  }
  
  private final Object get(final String mapKey, final Name name) throws NamingException {
    final Object returnValue;
    Object temp = null;
    try {
      temp = NamingManager.getObjectInstance(this.map.get(mapKey), name, this, this.environment);
    } catch (final RuntimeException throwMe) {
      throw throwMe;
    } catch (final NamingException throwMe) {
      throw throwMe;
    } catch (final Exception otherStuff) {
      throw (NamingException)new NamingException(otherStuff.getMessage()).initCause(otherStuff);
    } finally {
      returnValue = temp;
    }
    return returnValue;
  }

  private final String getMapKey(final Name name) throws NamingException {
    final Name compoundName = this.toCompoundName(Objects.requireNonNull(name));
    assert compoundName != null;
    final int size = compoundName.size();
    String component = null;
    for (int i = 0; i < size && (component == null || component.isEmpty()); i++) {
      component = compoundName.get(i);
    }
    final String returnValue;
    if (component == null || component.isEmpty()) {
      returnValue = "";
    } else {
      returnValue = component;
    }
    return returnValue;
  }

  @Override
  public Object lookup(final Name name) throws NamingException {
    Objects.requireNonNull(name);
    
    final Name compoundName = toCompoundName(name);
    assert compoundName != null;

    final Object returnValue;

    final int size = compoundName.size();
    if (size == 0) {
      assert compoundName.isEmpty();
      returnValue = this.copy();
    } else {
      assert size > 0;
      final String mapKey = this.getMapKey(compoundName);
      if (mapKey == null || mapKey.isEmpty()) {
        throw new InvalidNameException(name.toString());
      }
      final Object value = this.get(mapKey, compoundName);
      if (size == 1) {
        returnValue = value;
        if (returnValue == null) {
          throw new NameNotFoundException(name.toString());
        }
      } else if (value instanceof Context) {
        assert size > 1;
        returnValue = ((Context)value).lookup(compoundName.getSuffix(1));
      } else {
        throw new NotContextException(mapKey);
      }
    }
    
    return returnValue;
  }

  @Override
  public void bind(final Name name, final Object obj) throws NamingException {
    this.bind(name, obj, false);
  }

  @Override
  public void rebind(final Name name, final Object obj) throws NamingException {
    this.bind(name, obj, true);
  }

  private final void bind(final Name name, final Object obj, boolean rebindPermitted) throws NamingException {
    Objects.requireNonNull(name);
    if (name.isEmpty()) {
      throw new InvalidNameException("name.isEmpty()");
    }

    final Name compoundName = this.toCompoundName(name);
    assert compoundName != null;
    if (compoundName.isEmpty()) {
      throw new InvalidNameException(name.toString());
    }
    
    final int size = compoundName.size();
    assert size > 0;

    final String mapKey = this.getMapKey(compoundName);
    if (mapKey == null || mapKey.isEmpty()) {
      throw new InvalidNameException(name.toString());
    }
    
    final Object value = this.get(mapKey, compoundName);
    if (size == 1) {
      if (value != null) {
        if (rebindPermitted) {
          final Object old = this.map.remove(mapKey);
          assert old == value;
        } else {
          throw new NameAlreadyBoundException(name.toString());
        }
      }
      this.map.put(mapKey, NamingManager.getStateToBind(obj, this.getNameParser(compoundName).parse(mapKey), this, this.environment));
    } else if (value instanceof Context) {
      assert size > 1;
      ((Context)value).bind(compoundName.getSuffix(1), obj);
    } else {
      assert size > 1;
      throw new NotContextException(mapKey);
    }
	}

  @Override
  public void unbind(final Name name) throws NamingException {
    Objects.requireNonNull(name);
    if (name.isEmpty()) {
      throw new InvalidNameException("name.isEmpty()");
    }

    final Name compoundName = this.toCompoundName(name);
    assert compoundName != null;
    if (compoundName.isEmpty()) {
      throw new InvalidNameException(name.toString());
    }
    
    final int size = compoundName.size();
    assert size > 0;

    final String mapKey = this.getMapKey(compoundName);
    if (mapKey == null || mapKey.isEmpty()) {
      throw new InvalidNameException(name.toString());
    }

    if (size == 1) {
      this.map.remove(mapKey);
    } else {
      assert size > 1;
      final Object value = this.get(mapKey, compoundName);
      if (value instanceof Context) {
        ((Context)value).unbind(compoundName.getSuffix(1));
      } else {
        throw new NotContextException(mapKey);
      }
    }
	}

  @Override
  public Context createSubcontext(final Name name) throws NamingException {
    Objects.requireNonNull(name);
    if (name.isEmpty()) {
      throw new InvalidNameException("name.isEmpty()");
    }
    final Name compoundName = toCompoundName(name);
    assert compoundName != null;
    final int size = compoundName.size();
    assert size > 0;
    final String mapKey = this.getMapKey(compoundName);
    if (mapKey == null || mapKey.isEmpty()) {
      throw new InvalidNameException(name.toString());
    }
    if (this.map.containsKey(mapKey)) {
      throw new NameAlreadyBoundException(name.toString());
    }
    final Object value = this.get(mapKey, compoundName);
    final Context returnValue;
    if (size == 1) {
      returnValue = new MapContext(new HashMap<>(), this.environment, this.nameParser, name);
      final Object old = this.map.put(mapKey, returnValue);
      assert old == null;
    } else if (value instanceof Context) {
      assert size > 1;
      returnValue = ((Context)value).createSubcontext(compoundName.getSuffix(1));
    } else {
      throw new NotContextException(name.toString());
    }
    return returnValue;
  }

  @Override
  public void destroySubcontext(final Name name) throws NamingException {
    Objects.requireNonNull(name);
    if (name.isEmpty()) {
      throw new InvalidNameException("name.isEmpty()");
    }
    final Name compoundName = this.toCompoundName(name);
    assert compoundName != null;
    if (!compoundName.isEmpty()) {
      final String mapKey = this.getMapKey(compoundName);
      if (mapKey == null || mapKey.isEmpty()) {
        throw new InvalidNameException(name.toString());
      }
      final int size = compoundName.size();
      assert size > 0;
      Object value = this.get(mapKey, compoundName);
      if (size == 1) {
        if (value != null) {
          if (value instanceof Context) {
            final Context subcontext = (Context)value;
            final Enumeration<?> enumeration = subcontext.list("");
            if (enumeration != null && enumeration.hasMoreElements()) {
              throw new ContextNotEmptyException(name.toString());
            }
            subcontext.close();
            final Object old = this.map.remove(mapKey);
            assert old == subcontext;
          } else {
            throw new NotContextException(name.toString());
          }
        }
      } else if (value instanceof Context) {
        assert size > 1;
        ((Context)value).destroySubcontext(name.getSuffix(1));
      } else {
        throw new NotContextException(mapKey);
      }
    } else {
      // Specification says that this method should be a no-op if the
      // compound name is not bound in this context; an empty compound
      // name *is* this context so it is not bound.
    }
  }

  @Override
  public void rename(final Name oldName, final Name newName) throws NamingException {
    Objects.requireNonNull(oldName);
    Objects.requireNonNull(newName);
    if (oldName.isEmpty()) {
      throw new InvalidNameException("oldName.isEmpty()");
    } else if (newName.isEmpty()) {
      throw new InvalidNameException("newName.isEmpty()");
    }

    final String newKey = this.toCompoundName(newName).toString();
    if (this.map.containsKey(newKey)) {
      throw new NameAlreadyBoundException(newName.toString());
    }

    final Object oldKey = this.toCompoundName(oldName).toString();
    if (!this.map.containsKey(oldKey)) {
      throw new NameNotFoundException(oldName.toString());
    }
    
    this.map.put(newKey, this.map.remove(oldKey));
  }

  @Override
  public NamingEnumeration<NameClassPair> list(final Name name) throws NamingException {
    Objects.requireNonNull(name);
    final NamingEnumeration<NameClassPair> returnValue;
    if (name.isEmpty()) {
      returnValue = new NameClassPairEnumeration(this.map.keySet()) {
          @Override
          protected final Object get(final String n) throws NamingException {
            return MapContext.this.get(n);
          }
        };
    } else {
      final Object target = this.lookup(name);
      if (target instanceof Context) {
        final Context context = (Context)target;
        NamingEnumeration<NameClassPair> temp = null;
        try {
          temp = context.list(EMPTY_NAME);
        } finally {
          returnValue = temp;
        }
      } else {
        returnValue = null;
        throw new NotContextException(name + " cannot be listed");
      }
    }
    return returnValue;
  }

  @Override
  public NamingEnumeration<Binding> listBindings(final Name name) throws NamingException {
    Objects.requireNonNull(name);
    final NamingEnumeration<Binding> returnValue;
    if (name.isEmpty()) {
      returnValue = new BindingEnumeration(this.map.keySet()) {
          @Override
          protected final Object get(final String n) throws NamingException {
            return MapContext.this.get(n);
          }
        };
    } else {
      final Object target = this.lookup(name);
      if (target instanceof Context) {
        final Context context = (Context)target;
        NamingEnumeration<Binding> temp = null;
        try {
          temp = context.listBindings(EMPTY_NAME);
        } finally {
          returnValue = temp;
        }
      } else {
        returnValue = null;
        throw new NotContextException(name + " cannot be listed");
      }
    }
    return returnValue;
  }

  private static final class DefaultNameParser implements NameParser {

    private static final Properties defaultSyntax = Contexts.leftToRightSlashSeparatedSyntax();

    private DefaultNameParser() {
      super();
    }
    
    @Override
    public final Name parse(final String compoundNameString) throws NamingException {
      return new CompoundName(compoundNameString, defaultSyntax);
    }
  }
  
}
