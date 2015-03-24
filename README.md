# las2peer-annotationService
This microService stores annotations for objects in an ArangoDB graph database. Key concepts used in this service are:
* Objects are items which can be annotated. Typically metadata for these items is stored somewhere else, the service stores only a reference Id.
* Annotation contain the information for annotations. 
* AnnotationContext is a relation between an object and an annotation. These objects store information that relates the annotaion with the object, for instance position of the annotaion in the object, the time when the annotation is shown, its duration etc.

In ArangoDB Objects and Annotations are stored in collections, and AnnotationContexts are stored in edgeCollection. With this structure we have a graph where nodes are objects/annotations and edges are AnnotationContexts. The service is based on [las2peer](https://github.com/rwth-acis/LAS2peer).

##Requirements

* Installed Java 7 JDK
* Installed Apache Ant
* Installed [ArangoDB 2.4.5](https://www.arangodb.com/download)

##License
LAS2peer-annotationService is freely distributable under the [MIT License](https://github.com/rwth-acis/las2peer-annotationService/blob/master/LICENSE).


