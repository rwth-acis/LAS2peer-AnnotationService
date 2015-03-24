# Database

Create database using ArangoShell

## Create Database 
```
db._createDatabase('annotations', {}, []); 
```
##	Switch to the newly created database
```
db._useDatabase('annotations'); 
```
## Create User
```
require("org/arangodb/users").save('userName', 'password', true);
```
	
## Create a graph 
```
var graph_module = require("org/arangodb/general-graph");
var graph = graph_module._create("Video");
```
	
## Add some vertex collections
```
graph._addVertexCollection("Videos")
graph._addVertexCollection("Annotations")
```

## Define relations
```
var rel = graph_module._relation("Annotated", ["Videos","Annotations"], ["Annotations"]);
graph._extendEdgeDefinitions(rel);
```

###References
* [Create Database](https://github.com/arangodb/arangodb/issues/990)
* [Create Graph](https://docs.arangodb.com/General-Graphs/README.html)
* [Graph Management](https://docs.arangodb.com/General-Graphs/Management.html)	
