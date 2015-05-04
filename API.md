# API Explanation

Explanation of the API together with examples

## Table Of Content


## Get All Objects

### Get All objects of a specific collection
   ```GET /objects?collection=Videos```

#### Response

### Get All objects without specifying a collection
   ```GET /objects```

#### Response
	```JSON
	[
  {
    "id": "11737",
    "_rev": "278067813411",
    "_id": "Images/278067813411",
    "_key": "278067813411"
  },
  {
    "id": "10884",
    "_rev": "277044665379",
    "_id": "Images/277044665379",
    "_key": "277044665379"
  },
  {
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
  },
  {
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
  },
   {
    "id": "11634",
    "_rev": "277994675235",
    "_id": "Videos/277994675235",
    "_key": "277994675235"
  }
]
```

### Get All objects specifying part of the information to receive
   ```GET /objects?part=id,text```

#### Response


