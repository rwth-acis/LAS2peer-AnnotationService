# Sample API Requests

This page shows sample requests to the Annotation Service API. The response to each request is the JSON representation of a resource. The `part` parameter in the request specifies which portions of the resource are included in the response. For instance, for an annotation resource parts can be:  

* `id`  
* `author`  
* `text`  
* `title`  
* `keywords`  

 To include more than one part, combine values separated by `,`. 
For each insert in the database, it is stored automatically `author` and `timeStamp` information.
## Table Of Content
* [Retrieve Object Information](#retrieve-object-information)  
    * [Get All Objects](#get-all-objects) 
    * [Get All Objects of a Specific Collection](#get-all-objects-of-a-specific-collection)   
    * [Get a Specific Object](#get-a-specific-object) 
    * [Get Annotations of an Object](#get-annotations-of-an-object)  
    * [Get Annotations containing Keyword(s)](#get-annotations-containing-keywords)  
    * [Get AnnotationContexts](#get-annotationcontexts)  
    * [Get Collections](#get-collections)  
* [Storing Objects/Annotations](#storing-objectsannotations)  
	* [Store New Object](#store-new-object)  
	* [Store New Annotation](#store-new-annotation)  
	* [Store New AnnotationContext](#store-new-annotationcontext)  
* [Update Objects/Annotations](#update-objectsannotations)  
	* [Update Object](#update-object)  
	* [Update AnnotationContext](#update-annotationcontext)
* [Delete Objects/Annotations](#delete-objectsannotations)  
	* [Delete Object](#delete-object)  
	* [Delete AnnotationContext](#delete-annotationcontext)  

## Retrieve Object Information

### Get All Objects
  
```GET {base-url}/objects```

The response of this request is a JSON array containing information for all the objects in the database. For objects it returns the `id` value and in additional for annotations among others returns `author`, `title`, `text` etc..

```javascript
[{
	"id": "11737",
	"_rev": "278067813411",
	"_id": "Images/278067813411",
	"_key": "278067813411"
}, {
	"id": "10963",
	"_rev": "277119573027",
	"author": {
		"name": "adam",
		"uri": ""
	},
	"title": "Annotation Text Insert",
	"text": "",
	"keywords": "test annotation",
	"_id": "TextTypeAnnotation/277119573027",
	"annotationData": {
		"location": "Microservice Test Class"
	},
	"_key": "277119573027"
}, {
	"id": "11634",
	"_rev": "277994675235",
	"_id": "Videos/277994675235",
	"_key": "277994675235"
}]
```

### Get All Objects of a Specific Collection
   ```GET {base-url}/objects?collection={COLLECTION_NAME}```

This request would return all objects of `COLLECTION_NAME` collection. 

### Get a Specific Object
   ```GET {base-url}/objects/{objectId}```

This request will return all properties of `objectId`.

### Get Annotations of an Object

```GET {base-url}/objects/{objectId}/annotations```

This request will return all annotations of `objectId`. Paramerts here include:  

* Collection, (**optional**) to specify the type of annotations you need to retrieve.

The response will be:  

```javascript
{
  "annotations": [
    {
      "annotation": {...}
    },
    "annotationContexts": [
        {...},
		{...}
      ]
}
```

### Get Annotations containing Keyword(s)

```GET {base-url}/annotations?q={keyword1},{keyword2}```

This request will return all annotations and objects containing `{keyword1}`, `{keyword2}`. Parameters here include:  

* q, list of keywords
* part, (**optional**)
* Collection, (**optional**) to specify the type of annotations you need to retrieve.

The response will be:  

```javascript
[
  {
    "collection": "LocationTypeAnnotation",
    "contextId": "11460",
    "duration": "0.40",
    "id": "11019",
    "keywords": "test annotation",
    "location": "Aachen",
    "objectCollection": "Images",
    "objectId": "10848",
    "position": {
      "x": "10",
      "y": "10",
      "z": "10"
    },
    "text": "",
    "time": "1.324",
    "title": "Location Annotation Insert 10"
  }
]
```
### Get AnnotationContexts
```GET {base-url}/annotationContexts/{sourceId}/{destId}```

Retrieve annotationContext information between a `sourceId` and `destId`.

### Get Collections
```GET {base-url}/collections```

Retrieve collection names for all collections of the graph.

## Storing Objects/Annotations

### Store New Object
```javascript
POST {base-url}/objects
Request body:
{
	"collection": "{COLLECTION_NAME}"
}
```
### Store New Annotation
```javascript
POST {base-url}/annotations
Request body:
{
	"collection": "{COLLECTION_NAME}",
	"objectId": "{OBJECT_ID}"
}
```

This method will create a new annotation in the collection `COLLECTION_NAME` and in addition it will create an 'empty' annotationContext between the new annotation and `OBJECT_ID`. To the annotationContext will be assigned an `id`. The response will return `id` of the annotation and the id of the annotationContext in `annotationContextId`. Annotation can contain information like `title`, `text` etc. A complete list is specified in [Table 1](#new-annotation-request-body)

### Store New AnnotationContext
```javascript
POST {base-url}/annotationContexts/{sourceId}/{destId}
Request body:
{
	"position": { "x": "10", "y": "10", "z": "10"}, "time": "1.324", 
	"duration": "0.40"
}
```
This method will create an annotationContext between `sourceId` and `destId`. 

## Update Objects/Annotations

### Update Object
```javascript
PUT {base-url}/objects/{objectId}
Request body:
{
	"title": "This is an updated title"
}
```
### Update AnnotationContext
```javascript
PUT {base-url}/annotationContexts/{annotationContextId}
Request body:
{
	"duration": "1.70"
}
```

## Delete Objects/Annotations

### Delete Object
```javascript
DELETE {base-url}/objects/{objectId}
```
### Delete AnnotationContext
```javascript
DELETE {base-url}/annotationContexts/{annotationContextId}
```

### New Annotation Request Body
|Name   |Type   |Explanation   |
|-------|-------|--------------|
|title  |String |Title of the annotation  |
|text   |String |Text of the annotation   |
|keywords|String|List of keywords for this annotation|

**Important Note:** The user can include additional attributes in the request body. These additional fields will be stored under `annotationData`. For example, it one  additionally includes `"location": "Aachen"` in the request body, the annotation will look like this:
```javascript
{
"annotationData" :  {
      "location": "Aachen"
    },
"text" : "This is a sample text",
...
}
``` 
