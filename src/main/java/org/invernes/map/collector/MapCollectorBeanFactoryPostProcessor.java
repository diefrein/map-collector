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
 * Object of type <i>BeanFactoryPostProcessor</i>, that searches for beans of type <i>MapCollector</i> in context and set
 * dependencies on beans with annotation of type <i>annotationClass</i> and with type of <i>MapCollector</i> generic types
 * <p>
 * You simply need to add an instance of that class to context, Spring automatically calls method <i>postProcessBeanFactory</i>
 * todo links to classes and methods
 */
@Slf4j
@RequiredArgsConstructor
public class MapCollectorBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private final Class<? extends Annotation> annotationClass;

    /**
     * Метод, вызываемый спрингом после создания всех BeanDefinition, в котором устанавливаются зависмости для каждого MapCollector.
     * Зависимости устанавливаются следующим образом:
     * <p>
     * 1. Для каждого бина типа MapCollector определяется метод, которым он создается (это должен быть фабричный метод в файле конфигурации)
     * <p>
     * 2. Определяется generic параметр возвращаемого типа фабричного метода
     * <p>
     * 3. В контексте ищутся бины с аннотацией annotationClass и типа, определенного в п.2
     * <p>
     * 4. Каждый бин из п.3 устанавливается как зависимость для MapCollector
     *
     * @param beanFactory beanFactory, которую поставляет спринг
     * @throws RuntimeException если в контексте нет бинов типа MapCollector или не найдено фабричных методов, которым они создаются,
     *                          если не найдено бинов с аннотацией annotationClass
     */
    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String[] mapCollectorNames = beanFactory.getBeanNamesForType(MapCollector.class);
        if (mapCollectorNames.length == 0) {
            throw new RuntimeException("No beans of type MapCollector found, but MapCollectorBFPP is still used");
        }
        log.debug("{} beans of MapCollector type found", mapCollectorNames.length);
        for (String mapCollectorName : mapCollectorNames) {
            log.debug("Resolving dependencies for bean with name {}", mapCollectorName);
            Method factoryMethodForMapCollector = getFactoryMethodForBeanName(mapCollectorName, beanFactory);
            if (factoryMethodForMapCollector == null) {
                throw new RuntimeException(String.format("No factory method for bean with name %s found", mapCollectorName));
            }
            log.debug("Found factoryMethod for bean with name {}: {}", mapCollectorName, factoryMethodForMapCollector.getName());
            ResolvableType typeToCollect = getGenericReturnTypeOfMethod(factoryMethodForMapCollector);
            log.debug("Resolved return type of factoryMethod for bean with name {}: {}", mapCollectorName, typeToCollect);
            setDependsOn(mapCollectorName, typeToCollect, beanFactory);
        }
    }

    /**
     * Внутренний метод для поиска фабричного метода, которым создается бин с указанным beanName
     *
     * @param beanName    имя бина
     * @param beanFactory beanFactory, которую поставляет спринг
     * @return метод, которым создается бин или null если такой не найден
     */
    private Method getFactoryMethodForBeanName(String beanName,
                                               ConfigurableListableBeanFactory beanFactory) {
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
     * Определяет ResolvableType, которым параметризован возвращаемый тип метода
     * <p>
     * Важно! Суперкласс возвращаемого типа должен быть generic, наследник (возвращаемый тип) должен определять generic параметр
     * <p>
     * Например: ConverterMapCollector extends MapCollector&lt;Integer, Converter&gt;
     * <p>
     *
     * @param method объект метода (рефлексия)
     * @return обертка над типом, которым параметризован возвращаемый тип
     */
    private ResolvableType getGenericReturnTypeOfMethod(Method method) {
        Class<?> methodReturnType = method.getReturnType();
        Type methodReturnTypeArgument = ((ParameterizedType) methodReturnType.getGenericSuperclass()).getActualTypeArguments()[1];
        return ResolvableType.forType(methodReturnTypeArgument);
    }

    /**
     * Установка зависимостей бину с именем beanName. Бин считается зависимостью для бина с именем beanName, если:
     * <p>
     * 1. Он имеет аннотацию типа annotationClass
     * <p>
     * 2. Он имеет тот же тип, что и generic параметр у метода, которым создается MapCollector
     *
     * @param beanName       имя бина
     * @param dependencyType тип бина, от которого зависит бин с именем beanName
     * @param beanFactory    beanFactory, которую поставляет спринг
     */
    private void setDependsOn(String beanName,
                              ResolvableType dependencyType,
                              ConfigurableListableBeanFactory beanFactory) {
        String[] annotatedBeanNames = beanFactory.getBeanNamesForAnnotation(annotationClass);
        if (annotatedBeanNames.length == 0) {
            throw new RuntimeException("No beans with BusinessService annotation found");
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