package org.invernes.map.collector;

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
 * @implNote For more information check the built-in implementations // todo link to impl
 * @implSpec For correct usage extend this class with specific generic parameters:
 * class CustomMapCollector extends MapCollector&lt;Integer, SomeClass&gt;
 */
public abstract class MapCollector<K, V> implements BeanFactoryAware {

    private final Map<K, V> map = new HashMap<>();

    /** todo doc
     * @param beanFactory owning BeanFactory (never {@code null}).
     *                    The bean can immediately call methods on the factory.
     * @throws BeansException
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        var annotatedBeans = getAnnotatedBeans(beanFactory);
        for (var annotatedBeanEntry : annotatedBeans.entrySet()) {
            String beanName = annotatedBeanEntry.getKey();
            Object bean = annotatedBeanEntry.getValue();
            putMapEntries(beanName, bean, beanFactory);
        }
    }

    public Map<K, V> getMap() {
        return Collections.unmodifiableMap(map);
    }

    /** todo doc
     * @param clazz
     * @return
     */
    protected ResolvableType getClassGenerics(Class<?> clazz) {
        Type classTypeArgument = ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[1];
        return ResolvableType.forType(classTypeArgument);
    }

    /** todo doc
     * @param beanFactory
     * @return
     */
    protected abstract Map<String, Object> getAnnotatedBeans(BeanFactory beanFactory);

    /** todo doc
     * @param beanName
     * @param bean
     * @param beanFactory
     */
    protected abstract void putMapEntries(String beanName, Object bean, BeanFactory beanFactory);
}