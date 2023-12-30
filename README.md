# map-collector
<b>Service that provides collecting annotated beans to map at runtime.</b> 
<p>
Idea: annotate beans, that should be collected to a map, with an annotation, that defines with which key it will be stored.
</p>
<p>
Example: assume we have a map, that looks like that
</p>

``` java
@Bean
public Map<Integer, SomeType> someTypeMap(SomeType object1,
                                          SomeType object2,
                                          SomeType object3,
                                          ...) {
        return Map.of(1, object1,
                      2, object2,
                      3, object3
                      ...
        );
}
```
<p>
That is a good solution because Map.of() returns immutable map, and we are sure that object1, object2... are initialized when this factory method is executed.<br> 
A problem occurrs when the number of parameters grows. IDE marks that as warning and checkstyle complains about it, so you have to put @SupressWarning("ParameterNumber") on method, but that considered to be a bad practice in production code.
</p>
