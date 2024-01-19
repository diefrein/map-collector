package org.invernes.map.collector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;

import java.util.HashMap;
import java.util.Map;

@DisplayName("Unit tests for class MapCollector")
class MapCollectorTest {

    @Test
    @DisplayName("Test for method setBeanFactory")
    void setBeanFactoryTest() {

    }

    private @interface TestCollectAnnotation {
        int[] keys();
    }

    private static class TestClassToCollect {

    }

    private static class TestMapCollector extends MapCollector<Integer, TestClassToCollect> {

        @Override
        protected Map<String, Object> getAnnotatedBeans(BeanFactory beanFactory) {
            Map<String, Object> beansWithAnnotation =
                    ((ConfigurableListableBeanFactory) beanFactory).getBeansWithAnnotation(TestCollectAnnotation.class);
            ResolvableType typeToCollect = getClassGenerics(this.getClass());
            Map<String, Object> beansWithAnnotationAndOfType = new HashMap<>();
            for (Map.Entry<String, Object> beanEntry : beansWithAnnotation.entrySet()) {
                if (typeToCollect.isInstance(beanEntry.getValue())) {
                    beansWithAnnotationAndOfType.put(beanEntry.getKey(), beanEntry.getValue());
                }
            }
            return beansWithAnnotationAndOfType;
        }

        @Override
        protected void putMapEntries(String beanName, Object bean, BeanFactory beanFactory) {
            TestCollectAnnotation annotationOnBean =
                    ((ConfigurableListableBeanFactory) beanFactory).findAnnotationOnBean(beanName, TestCollectAnnotation.class);
            if (annotationOnBean == null) {
                throw new RuntimeException(String.format("TestCollectAnnotation not found on bean with name %s", beanName));
            }
            int[] keysForBean = annotationOnBean.keys();
            for (int key : keysForBean) {
                getMap().put(key, (TestClassToCollect) bean);
            }
        }
    }
}