# Sample API Requests

This page shows sample requests to the Annotation Service API. The response to each request is the JSON representation of a resource. The `part` parameter in the request specifies which portions of the resource are included in the response. For instance, for an annotation resource parts can be:  

* `id`  
* `author`  
* `text`  
* `title`  
* `keywords`  

## Table Of Content
* [Retrieve Object Information](#retrieve-object-information)  
    * [Get All Objects](#get-all-objects) 
    * [Get All Objects of a Specific Collection](#get-all-objects-of-a-specific-collection)    
[Build](#build)  
[Start](#start)  
[License](#license)

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
