package org.invernes.map.collector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for class MapCollectorBeanFactoryPostProcessor")
class MapCollectorBeanFactoryPostProcessorTest {

    private final Class<? extends Annotation> annotationClass = TestAnnotation.class;
    private final ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
    private final MapCollectorBeanFactoryPostProcessor sut = new MapCollectorBeanFactoryPostProcessor(annotationClass);

    @Test
    @DisplayName("Case when no beans of type MapCollector found in context. Exception thrown")
    void postProcessBeanFactory_NoMapCollectorsFound() {
        when(beanFactory.getBeanNamesForType(MapCollector.class)).thenReturn(new String[]{});

        String expectedExceptionMessage = "No beans of type MapCollector found, but MapCollectorBeanFactoryPostProcessor is still used";
        Exception exceptionThrown = assertThrows(MapCollectorException.class, () -> sut.postProcessBeanFactory(beanFactory));
        assertEquals(expectedExceptionMessage, exceptionThrown.getMessage());

        verify(beanFactory).getBeanNamesForType(MapCollector.class);
        verifyNoMoreInteractions(beanFactory);
    }

    @Test
    @DisplayName("Case when no factory bean for bean with type MapCollector found. Exception thrown")
    void postProcessBeanFactory_NoFactoryBeanFound() {
        String mapCollectorName = randomFromUuid();
        String[] mapCollectorNames = new String[]{mapCollectorName};
        when(beanFactory.getBeanNamesForType(MapCollector.class)).thenReturn(mapCollectorNames);

        BeanDefinition mapCollectorBeanDefinition = new RootBeanDefinition();
        when(beanFactory.getBeanDefinition(mapCollectorName)).thenReturn(mapCollectorBeanDefinition);

        String expectedExceptionMessage = String.format("No factory method for bean with name %s found", mapCollectorName);
        Exception exceptionThrown = assertThrows(MapCollectorException.class, () -> sut.postProcessBeanFactory(beanFactory));
        assertEquals(expectedExceptionMessage, exceptionThrown.getMessage());

        verify(beanFactory).getBeanNamesForType(MapCollector.class);
        verify(beanFactory).getBeanDefinition(mapCollectorName);
        verifyNoMoreInteractions(beanFactory);
    }

    @Test
    @DisplayName("Case when no factory method for bean with type MapCollector found. Exception thrown")
    void postProcessBeanFactory_NoFactoryMethodFound() {
        String mapCollectorName = randomFromUuid();
        String[] mapCollectorNames = new String[]{mapCollectorName};
        when(beanFactory.getBeanNamesForType(MapCollector.class)).thenReturn(mapCollectorNames);

        String factoryBeanName = randomFromUuid();
        BeanDefinition mapCollectorBeanDefinition = new RootBeanDefinition();
        mapCollectorBeanDefinition.setFactoryBeanName(factoryBeanName);
        when(beanFactory.getBeanDefinition(mapCollectorName)).thenReturn(mapCollectorBeanDefinition);

        BeanDefinition factoryBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(TestConfigurationClass.class).getBeanDefinition();
        when(beanFactory.getBeanDefinition(factoryBeanName)).thenReturn(factoryBeanDefinition);

        String expectedExceptionMessage = String.format("No factory method for bean with name %s found", mapCollectorName);
        Exception exceptionThrown = assertThrows(MapCollectorException.class, () -> sut.postProcessBeanFactory(beanFactory));
        assertEquals(expectedExceptionMessage, exceptionThrown.getMessage());

        verify(beanFactory).getBeanNamesForType(MapCollector.class);
        verify(beanFactory).getBeanDefinition(mapCollectorName);
        verify(beanFactory).getBeanDefinition(factoryBeanName);
        verifyNoMoreInteractions(beanFactory);
    }

    @Test
    @DisplayName("Case when no beans with required annotation found. Exception thrown")
    void postProcessBeanFactory_NoAnnotatedBeansFound() {
        String mapCollectorName = randomFromUuid();
        String[] mapCollectorNames = new String[]{mapCollectorName};
        when(beanFactory.getBeanNamesForType(MapCollector.class)).thenReturn(mapCollectorNames);

        String factoryBeanName = randomFromUuid();
        BeanDefinition mapCollectorBeanDefinition = new RootBeanDefinition();
        mapCollectorBeanDefinition.setFactoryBeanName(factoryBeanName);
        mapCollectorBeanDefinition.setFactoryMethodName(TestConfigurationClass.class.getMethods()[0].getName());
        when(beanFactory.getBeanDefinition(mapCollectorName)).thenReturn(mapCollectorBeanDefinition);

        BeanDefinition factoryBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(TestConfigurationClass.class).getBeanDefinition();
        when(beanFactory.getBeanDefinition(factoryBeanName)).thenReturn(factoryBeanDefinition);

        String[] annotatedBeanNames = new String[]{};
        when(beanFactory.getBeanNamesForAnnotation(annotationClass)).thenReturn(annotatedBeanNames);

        assertThrows(MapCollectorException.class, () -> sut.postProcessBeanFactory(beanFactory));

        verify(beanFactory).getBeanNamesForType(MapCollector.class);
        verify(beanFactory).getBeanDefinition(mapCollectorName);
        verify(beanFactory).getBeanDefinition(factoryBeanName);
        verify(beanFactory).getBeanNamesForAnnotation(annotationClass);
        verifyNoMoreInteractions(beanFactory);
    }

    @Test
    @DisplayName("Case when beans to collect are not annotated. No beans are set as dependencies")
    void postProcessBeanFactory_NeededBeansAreNotAnnotated() {
        String mapCollectorName = randomFromUuid();
        String[] mapCollectorNames = new String[]{mapCollectorName};
        when(beanFactory.getBeanNamesForType(MapCollector.class)).thenReturn(mapCollectorNames);

        String factoryBeanName = randomFromUuid();
        BeanDefinition mapCollectorBeanDefinition = new RootBeanDefinition();
        mapCollectorBeanDefinition.setFactoryBeanName(factoryBeanName);
        mapCollectorBeanDefinition.setFactoryMethodName(TestConfigurationClass.class.getMethods()[0].getName());
        when(beanFactory.getBeanDefinition(mapCollectorName)).thenReturn(mapCollectorBeanDefinition);

        BeanDefinition factoryBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(TestConfigurationClass.class).getBeanDefinition();
        when(beanFactory.getBeanDefinition(factoryBeanName)).thenReturn(factoryBeanDefinition);

        String[] annotatedBeanNames = new String[]{randomFromUuid()};
        when(beanFactory.getBeanNamesForAnnotation(annotationClass)).thenReturn(annotatedBeanNames);

        Type methodReturnTypeArgument = ((ParameterizedType) TestMapCollector.class.getGenericSuperclass()).getActualTypeArguments()[1];
        ResolvableType dependencyType = ResolvableType.forType(methodReturnTypeArgument);
        String[] beanNamesToCollect = new String[]{randomFromUuid()};
        when(beanFactory.getBeanNamesForType(dependencyType)).thenReturn(beanNamesToCollect);

        sut.postProcessBeanFactory(beanFactory);

        assertNull(mapCollectorBeanDefinition.getDependsOn());

        verify(beanFactory).getBeanNamesForType(MapCollector.class);
        verify(beanFactory, times(2)).getBeanDefinition(mapCollectorName);
        verify(beanFactory).getBeanDefinition(factoryBeanName);
        verify(beanFactory).getBeanNamesForAnnotation(annotationClass);
        verify(beanFactory).getBeanNamesForType(dependencyType);
        verifyNoMoreInteractions(beanFactory);
    }

    @Test
    @DisplayName("Case when beans to collect are properly annotated. Beans are set as dependencies")
    void postProcessBeanFactory_AnnotatedBeansFound() {
        String mapCollectorName = randomFromUuid();
        String[] mapCollectorNames = new String[]{mapCollectorName};
        when(beanFactory.getBeanNamesForType(MapCollector.class)).thenReturn(mapCollectorNames);

        String factoryBeanName = randomFromUuid();
        BeanDefinition mapCollectorBeanDefinition = new RootBeanDefinition();
        mapCollectorBeanDefinition.setFactoryBeanName(factoryBeanName);
        mapCollectorBeanDefinition.setFactoryMethodName(TestConfigurationClass.class.getMethods()[0].getName());
        when(beanFactory.getBeanDefinition(mapCollectorName)).thenReturn(mapCollectorBeanDefinition);

        BeanDefinition factoryBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(TestConfigurationClass.class).getBeanDefinition();
        when(beanFactory.getBeanDefinition(factoryBeanName)).thenReturn(factoryBeanDefinition);

        String[] annotatedBeanNames = new String[]{randomFromUuid()};
        when(beanFactory.getBeanNamesForAnnotation(annotationClass)).thenReturn(annotatedBeanNames);

        Type methodReturnTypeArgument = ((ParameterizedType) TestMapCollector.class.getGenericSuperclass()).getActualTypeArguments()[1];
        ResolvableType dependencyType = ResolvableType.forType(methodReturnTypeArgument);
        String[] beanNamesToCollect = Arrays.copyOf(annotatedBeanNames, annotatedBeanNames.length);
        when(beanFactory.getBeanNamesForType(dependencyType)).thenReturn(beanNamesToCollect);

        sut.postProcessBeanFactory(beanFactory);

        assertNotNull(mapCollectorBeanDefinition.getDependsOn());
        assertEquals(1, mapCollectorBeanDefinition.getDependsOn().length);

        verify(beanFactory).getBeanNamesForType(MapCollector.class);
        verify(beanFactory, times(2)).getBeanDefinition(mapCollectorName);
        verify(beanFactory).getBeanDefinition(factoryBeanName);
        verify(beanFactory).getBeanNamesForAnnotation(annotationClass);
        verify(beanFactory).getBeanNamesForType(dependencyType);
        verifyNoMoreInteractions(beanFactory);
    }

    private String randomFromUuid() {
        return UUID.randomUUID().toString();
    }

    private @interface TestAnnotation {

    }

    private static class TestConfigurationClass {

        public TestMapCollector factoryMethod() {
            return null;
        }
    }

    private static class TestMapCollector extends MapCollector<Integer, TestClassToCollect> {

        @Override
        protected Map<String, Object> getAnnotatedBeans(BeanFactory beanFactory) {
            return null;
        }

        @Override
        protected void putMapEntries(String beanName, Object bean, BeanFactory beanFactory) {

        }
    }

    private static class TestClassToCollect {

    }
}