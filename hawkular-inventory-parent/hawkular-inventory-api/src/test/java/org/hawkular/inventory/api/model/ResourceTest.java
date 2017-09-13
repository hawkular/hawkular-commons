package org.hawkular.inventory.api.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Joel Takvorian
 */
public class ResourceTest {

    private final Resource r = new Resource("id", "name", "EAP", "feed", null,
            newList("child-1", "child-2"), newList("m-1", "m-2"), new HashMap<>());

    private static <T> List<T> newList(T... args) {
        List<T> l = new ArrayList<>();
        l.addAll(Arrays.asList(args));
        return l;
    }

    @Test
    public void shouldLazyLoadResourceType() {
        ResourceType eap = new ResourceType("EAP", "feed", new ArrayList<>(), new HashMap<>());
        LongAdder numberOfCalls = new LongAdder();
        Function<String, ResourceType> loader = id -> {
            numberOfCalls.increment();
            return eap;
        };

        ResourceType result = r.getType(loader);
        Assert.assertEquals("EAP", result.getId());
        Assert.assertEquals(1, numberOfCalls.intValue());
        // Verify loader is not called again
        r.getType(loader);
        Assert.assertEquals(1, numberOfCalls.intValue());
    }

    @Test
    public void shouldLazyLoadChildren() {
        Resource child1 = new Resource("child-1", "name-1", "t", "feed", "id",
                new ArrayList<>(), new ArrayList<>(), new HashMap<>());
        Resource child2 = new Resource("child-2", "name-2", "t", "feed", "id",
                new ArrayList<>(), new ArrayList<>(), new HashMap<>());
        LongAdder numberOfCalls = new LongAdder();
        Function<String, Resource> loader = id -> {
            numberOfCalls.increment();
            if (id.equals(child1.getId())) {
                return child1;
            }
            return child2;
        };

        List<Resource> children = r.getChildren(loader);
        Assert.assertEquals("name-1", children.get(0).getName());
        Assert.assertEquals("name-2", children.get(1).getName());
        Assert.assertEquals(2, numberOfCalls.intValue());
        // Verify loader is not called again
        r.getChildren(loader);
        Assert.assertEquals(2, numberOfCalls.intValue());
    }

    @Test
    public void shouldLazyLoadMetrics() {
        Metric m1 = new Metric("m-1", "name-1", "feed", MetricUnit.BYTES, 10,
                new HashMap<>());
        Metric m2 = new Metric("m-2", "name-2", "feed", MetricUnit.BYTES, 10,
                new HashMap<>());
        LongAdder numberOfCalls = new LongAdder();
        Function<String, Metric> loader = id -> {
            numberOfCalls.increment();
            if (id.equals(m1.getId())) {
                return m1;
            }
            return m2;
        };

        List<Metric> metrics = r.getMetrics(loader);
        Assert.assertEquals("name-1", metrics.get(0).getName());
        Assert.assertEquals("name-2", metrics.get(1).getName());
        Assert.assertEquals(2, numberOfCalls.intValue());
        // Verify loader is not called again
        r.getMetrics(loader);
        Assert.assertEquals(2, numberOfCalls.intValue());
    }
}
