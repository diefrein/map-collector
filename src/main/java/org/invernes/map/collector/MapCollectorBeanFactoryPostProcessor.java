package org.invernes.map.collector;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.ResolvableType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Class implementing {@link BeanFactoryPostProcessor}, that searches for beans of type {@link MapCollector} in context and set
 * dependencies on beans with annotation of type <i>annotationClass</i> and with type of {@link MapCollector} generic types
 * <p>
 * You simply need to add an instance of that class to context, Spring automatically calls method
 * {@link BeanFactoryPostProcessor#postProcessBeanFactory(ConfigurableListableBeanFactory) postProcessBeanFactory}
 */
@Slf4j
@RequiredArgsConstructor
public class MapCollectorBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private final Class<? extends Annotation> annotationClass;

    /**
     * Method called by Spring after creating all {@link BeanDefinition}, in which for each {@link MapCollector}
     * dependencies are set
     * <p>
     * Dependencies are set as follows:
     * <p>
     * 1. For each bean of type {@link MapCollector} factory method is defined, with which it is created
     * 2. {@link MapCollectorBeanFactoryPostProcessor} defines generic of the return type of found factory method
     * <p>
     * 3. {@link MapCollectorBeanFactoryPostProcessor} searches for beans with type defined in p.2
     * and with annotation of type <i>annotationClass</i> are found
     * <p>
     * 4. Each bean found in p.3 is set as dependency of {@link MapCollector}
     *
     * @param beanFactory beanFactory, provided by Spring
     * @throws MapCollectorException if context doesn't contain beans of type {@link MapCollector},
     *                               if there are no fabric methods with which {@link MapCollector} are created,
     *                               if there are no beans with annotation of type <i>annotationClass</i> found
     */
    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String[] mapCollectorNames = beanFactory.getBeanNamesForType(MapCollector.class);
        if (mapCollectorNames.length == 0) {
            throw new MapCollectorException("No beans of type MapCollector found, but MapCollectorBeanFactoryPostProcessor is still used");
        }
        log.debug("{} beans of MapCollector type found", mapCollectorNames.length);
        for (String mapCollectorName : mapCollectorNames) {
            log.debug("Resolving dependencies for bean with name {}", mapCollectorName);
            Method factoryMethodForMapCollector = getFactoryMethodForBeanName(mapCollectorName, beanFactory);
            if (factoryMethodForMapCollector == null) {
                throw new MapCollectorException(String.format("No factory method for bean with name %s found", mapCollectorName));
            }
            log.debug("Found factoryMethod for bean with name {}: {}", mapCollectorName, factoryMethodForMapCollector.getName());
            ResolvableType typeToCollect = getGenericReturnTypeOfMethod(factoryMethodForMapCollector);
            log.debug("Resolved return type of factoryMethod for bean with name {}: {}", mapCollectorName, typeToCollect);
            setDependsOn(mapCollectorName, typeToCollect, beanFactory);
        }
    }

    /**
     * Internal method to search for fabric method of bean with name <i>beanName</i>
     *
     * @param beanName    name of bean
     * @param beanFactory beanFactory, provided be Spring
     * @return bean's factory method or null if not found
     */
    private Method getFactoryMethodForBeanName(String beanName, ConfigurableListableBeanFactory beanFactory) {
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        String factoryBeanName = beanDefinition.getFactoryBeanName();
        if (factoryBeanName == null) {
            return null;
        }
        String factoryMethodName = beanDefinition.getFactoryMethodName();
        BeanDefinition factoryBeanDefinition = beanFactory.getBeanDefinition(factoryBeanName);
        for (Method factoryMethod : ((GenericBeanDefinition) factoryBeanDefinition).getBeanClass().getMethods()) {
            if (factoryMethod.getName().equals(factoryMethodName)) {
                return factoryMethod;
            }
        }
        return null;
    }

    /**
     * Internal method to get ResolvableType of the generic parameter of method's return type
     *
     * @param method {@link Method} object
     * @return {@link ResolvableType} of generic parameter
     * @implSpec Superclass of method's return type should be generic
     * Example: SomeClassMapCollector extends MapCollector&lt;Integer, SomeClass&gt;
     * <p>
     */
    private ResolvableType getGenericReturnTypeOfMethod(Method method) {
        Class<?> methodReturnType = method.getReturnType();
        Type methodReturnTypeArgument = ((ParameterizedType) methodReturnType.getGenericSuperclass()).getActualTypeArguments()[1];
        return ResolvableType.forType(methodReturnTypeArgument);
    }

    /**
     * Internal method to set dependencies for bean with name <i>beanName</i>
     *
     * @param beanName       name of bean
     * @param dependencyType type of bean to be set as dependency for bean with name <i>beanName</i>
     * @param beanFactory    beanFactory, provided by Spring
     */
    private void setDependsOn(String beanName,
                              ResolvableType dependencyType,
                              ConfigurableListableBeanFactory beanFactory) {
        String[] annotatedBeanNames = beanFactory.getBeanNamesForAnnotation(annotationClass);
        if (annotatedBeanNames.length == 0) {
            throw new MapCollectorException(String.format("No beans with annotation of type %s found", annotationClass.getName()));
        }
        String[] beanNamesToCollect = beanFactory.getBeanNamesForType(dependencyType);
        List<String> actuallySetDependencies = new ArrayList<>();
        List<String> notSetDependencies = new ArrayList<>();
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        for (String beanNameToCollect : beanNamesToCollect) {
            if (contains(annotatedBeanNames, beanNameToCollect)) {
                beanDefinition.setDependsOn(beanNameToCollect);
                actuallySetDependencies.add(beanNameToCollect);
            } else {
                notSetDependencies.add(beanNameToCollect);
            }
        }
        log.info("Following dependencies (count: {}) were set for bean with name {}: {}",
                actuallySetDependencies.size(), beanName, String.join(", ", actuallySetDependencies));
        if (actuallySetDependencies.size() < beanNamesToCollect.length) {
            log.warn("Number of set dependencies (count: {}) is less than number of beans with type to collect (count: {}). "
                            + "Bean names with proper type, but not set as dependencies: {}",
                    actuallySetDependencies.size(), notSetDependencies.size(), String.join(",", notSetDependencies));
        }
    }

    private <T> boolean contains(T[] array, T target) {
        for (T value : array) {
            if (value.equals(target)) {
                return true;
            }
        }
        return false;
    }
}