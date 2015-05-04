# API Explanation

Explanation of the API together with examples

## Table Of Content
* [Get All Objects](#get-all-objects)  
	* [Get All Objects of a Specific Collection](#get-all-objects-of-a-specific-collection)  
	* [Get All Objects Without Specifying a Collection](#get-all-objects-without-specifying-a-collection)  
	* [Get All Objects Specifying Part of the Information to Receive](#get-all-objects-specifying-part-of-the-information-to-receive)  
[Build](#build)  
[Start](#start)  
[License](#license)

## Get All Objects

### Get All Objects of a Specific Collection
   ```GET /objects?collection=Videos```

#### Response
```javascript
[{
	"id": "11634",
	"_rev": "277994675235",
	"_id": "Videos/277994675235",
	"_key": "277994675235"
}]
```
### Get All Objects Without Specifying a Collection
   ```GET /objects```

#### Response
```javascript
[{
	"id": "11737",
	"_rev": "278067813411",
	"_id": "Images/278067813411",
	"_key": "278067813411"
}, {
	"id": "10884",
	"_rev": "277044665379",
	"_id": "Images/277044665379",
	"_key": "277044665379"
}, {
	"id": "11947",
	"_rev": "278241221667",
	"author": {
		"name": "adam",
		"uri": ""
	},
	"title": "Location Annotation Insert 28",
	"text": "",
	"keywords": "test annotation",
	"_id": "LocationTypeAnnotation/278241221667",
	"annotationData": {
		"location": "Aachen"
	},
	"_key": "278241221667"
}, {
	"id": "10963",
	"_rev": "277119573027",
	"author": {
		"name": "adam",
		"uri": ""
	},
	"title": "Annotation Text Insert 54",
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

### Get All Objects Specifying Part of the Information to Receive
   ```GET /objects?part=id,text```

#### Response
```javascript
[{
	"id": "11737",
	"text": null
}, {
	"id": "10884",
	"text": null
}, {
	"id": "11947",
	"text": ""
}, {
	"id": "10963",
	"text": ""
}, {
	"id": "11634",
	"text": null
}]
```

