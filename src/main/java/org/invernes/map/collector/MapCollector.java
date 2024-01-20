package org.invernes.map.collector;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.ResolvableType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that collects annotatedBeans
 *
 * @param <K> key type of the map
 * @param <V> value type of the map
 * @implNote For use examples see tests org.invernes.map.collector.MapCollectorTest
 * @implSpec For correct usage extend this class with specific generic parameters:
 * class SomeClassMapCollector extends MapCollector&lt;Integer, SomeClass&gt;
 */
@RequiredArgsConstructor
public abstract class MapCollector<K, V> implements BeanFactoryAware {

    protected final Map<K, V> map = new HashMap<>();

    /**
     * @param beanFactory owning {@link BeanFactory} (never {@code null}).
     *                    The bean can immediately call methods on the factory.
     * @throws BeansException in case of initialization errors
     */
    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        var annotatedBeans = getAnnotatedBeans(beanFactory);
        for (var annotatedBeanEntry : annotatedBeans.entrySet()) {
            String beanName = annotatedBeanEntry.getKey();
            Object bean = annotatedBeanEntry.getValue();
            ResolvableType typeToCollect = getClassGenerics(this.getClass());
            if (typeToCollect.isInstance(bean)) {
                putMapEntries(beanName, bean, beanFactory);
            }
        }
    }

    public Map<K, V> getMap() {
        return Collections.unmodifiableMap(map);
    }

    /**
     * Internal method to get generic parameter of specified class
     *
     * @param clazz specified class
     * @return {@link ResolvableType} of class generic
     */
    protected ResolvableType getClassGenerics(Class<?> clazz) {
        Type classTypeArgument = ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[1];
        return ResolvableType.forType(classTypeArgument);
    }

    /**
     * Internal method to get beans with specified annotation
     *
     * @param beanFactory owning {@link BeanFactory}
     * @return map of annotated beans with bean names as keys
     */
    protected abstract Map<String, Object> getAnnotatedBeans(BeanFactory beanFactory);

    /**
     * Internal method to put entries into map
     *
     * @param beanName    name of bean to be put into map
     * @param bean        bean
     * @param beanFactory owning {@link BeanFactory}
     */
    protected abstract void putMapEntries(String beanName, Object bean, BeanFactory beanFactory);
}