package org.invernes.map.collector;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

@TestConfiguration
class MapCollectorTestConfiguration {

    public static final TestClassToCollect INSTANCE1 = new TestClassToCollect();
    public static final TestClassToCollect INSTANCE2 = new TestClassToCollect();
    public static final TestClassToCollect INSTANCE3 = new TestClassToCollect();

    @Bean
    public Map<Integer, TestClassToCollect> testMap(TestMapCollector testMapCollector) {
        return testMapCollector.getMap();
    }

    @Bean
    @TestCollectAnnotation(keys = 1)
    public TestClassToCollect instance1() {
        return INSTANCE1;
    }

    @Bean
    @TestCollectAnnotation(keys = {2, 3})
    public TestClassToCollect instance2() {
        return INSTANCE2;
    }

    // no annotation
    @Bean
    public TestClassToCollect instance3() {
        return INSTANCE3;
    }

    @Bean
    public TestMapCollector testMapCollector() {
        return new TestMapCollector();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface TestCollectAnnotation {
        int[] keys();
    }

    static class TestClassToCollect {

    }

    static class TestMapCollector extends MapCollector<Integer, TestClassToCollect> {

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
                map.put(key, (TestClassToCollect) bean);
            }
        }
    }
}