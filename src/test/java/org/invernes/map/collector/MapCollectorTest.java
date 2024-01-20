package org.invernes.map.collector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisplayName("ITs for class MapCollector")
@Import(MapCollectorTestConfiguration.class)
@SpringBootTest(classes = MapCollectorTest.class)
class MapCollectorTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Test for method setBeanFactory")
    void setBeanFactoryTest() {
        Map<Integer, MapCollectorTestConfiguration.TestClassToCollect> expectedMap = Map.of(
                1, MapCollectorTestConfiguration.INSTANCE1,
                2, MapCollectorTestConfiguration.INSTANCE2,
                3, MapCollectorTestConfiguration.INSTANCE2
        );

        var actualMap = applicationContext.getBean("testMap");
        assertTrue(actualMap instanceof Map);
        assertEquals(expectedMap, actualMap);
    }
}