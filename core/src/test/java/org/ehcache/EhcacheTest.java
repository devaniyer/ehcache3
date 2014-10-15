/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehcache;

import org.ehcache.events.StateChangeListener;
import org.ehcache.exceptions.StateTransitionException;
import org.ehcache.spi.cache.Store;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

public class EhcacheTest {

  @Test
  public void testTransistionsState() {
    Store store = mock(Store.class);
    Ehcache ehcache = new Ehcache(store);
    assertThat(ehcache.getStatus(), is(Status.UNINITIALIZED));
    ehcache.init();
    assertThat(ehcache.getStatus(), is(Status.AVAILABLE));
    ehcache.close();
    assertThat(ehcache.getStatus(), is(Status.UNINITIALIZED));
    ehcache.toMaintenance();
    assertThat(ehcache.getStatus(), is(Status.MAINTENANCE));
    ehcache.close();
    assertThat(ehcache.getStatus(), is(Status.UNINITIALIZED));
  }

  @Test
  public void testThrowsWhenNotAvailable() {
    Store store = mock(Store.class);
    Ehcache ehcache = new Ehcache(store);

    try {
      ehcache.get("foo");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }

    try {
      ehcache.put("foo", "bar");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }

    try {
      ehcache.remove("foo");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }

    try {
      ehcache.remove("foo", "bar");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }

    try {
      ehcache.containsKey("foo");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }

    try {
      ehcache.replace("foo", "bar");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }

    try {
      ehcache.replace("foo", "foo", "bar");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }

    try {
      ehcache.putIfAbsent("foo", "bar");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }

    try {
      ehcache.clear();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }

    try {
      ehcache.iterator();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }

    try {
      ehcache.getAll(Collections.singleton("foo"));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }

    try {
      ehcache.removeAll(Collections.singleton("foo"));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }

    try {
      ehcache.putAll(Collections.singletonMap("foo", "bar").entrySet());
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }

    ehcache.init();
    final Iterator iterator = ehcache.iterator();
    ehcache.close();
    try {
      iterator.hasNext();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }
    try {
      iterator.next();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }
    try {
      iterator.remove();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage().contains(Status.UNINITIALIZED.name()), is(true));
    }
  }

  @Test
  public void testDelegatesLifecycleCallsToStore() {
    final Store store = mock(Store.class);
    Ehcache ehcache = new Ehcache(store);
    ehcache.init();
    verify(store).init();
    ehcache.close();
    verify(store).close();
    ehcache.toMaintenance();
    verify(store).maintenance();
  }

  @Test
  public void testFailingTransitionGoesToLowestStatus() {
    final Store store = mock(Store.class);
    Ehcache ehcache = new Ehcache(store);
    doThrow(new RuntimeException()).when(store).init();
    try {
      ehcache.init();
      fail();
    } catch (StateTransitionException e) {
      assertThat(ehcache.getStatus(), is(Status.UNINITIALIZED));
    }

    reset(store);
    ehcache.init();
    assertThat(ehcache.getStatus(), is(Status.AVAILABLE));
    doThrow(new RuntimeException()).when(store).close();
    try {
      ehcache.close();
      fail();
    } catch (StateTransitionException e) {
      assertThat(ehcache.getStatus(), is(Status.UNINITIALIZED));
    }

    doThrow(new RuntimeException()).when(store).maintenance();
    try {
      ehcache.toMaintenance();
      fail();
    } catch (StateTransitionException e) {
      assertThat(ehcache.getStatus(), is(Status.UNINITIALIZED));
    }

    reset(store);
    ehcache.toMaintenance();
    assertThat(ehcache.getStatus(), is(Status.MAINTENANCE));
    doThrow(new RuntimeException()).when(store).close();
    try {
      ehcache.close();
      fail();
    } catch (StateTransitionException e) {
      assertThat(ehcache.getStatus(), is(Status.UNINITIALIZED));
    }
  }

  @Test
  public void testFiresListener() {
    Store store = mock(Store.class);
    Ehcache ehcache = new Ehcache(store);

    final StateChangeListener listener = mock(StateChangeListener.class);
    ehcache.registerListener(listener);
    ehcache.init();
    verify(listener).stateTransition(Status.UNINITIALIZED, Status.AVAILABLE);
    reset(listener);
    ehcache.deregisterListener(listener);
    ehcache.close();
    verify(listener, never()).stateTransition(Status.AVAILABLE, Status.UNINITIALIZED);
  }

}