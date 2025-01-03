# NormalizerFieldValue - SMT for Kafka Connect / Debezium
A Kafka Connect SMT that allows you to add the record field to the value normalizer for Vietnamese text.

## Overview
The `NormalizerFieldValue` transformation is designed to enhance Kafka Connect functionality by add new field as a normalizer field value within the record's value. 

## Features
* Add the normalizer field value form a define field value.
* Customizable field name and filters (strip_html,noise_char,long_space,...).

## Installation
1. Use the latest release on GitHub, or build the JAR file from source using Maven:
```bash
mvn clean package
```
2. Copy the generated JAR file (`normalizer-field-value-transform-<version>.jar`) to the Kafka Connect plugins directory.
3. Restart Kafka Connect for the reload the plugin directory.
4. Update your connector with the SMT configuration

## Configuration
The KeyToField transformation can be configured with the following properties:

* `field.name`: Name of the field to insert the Kafka key to (default: `kafkaKey`).
* `field.delimiter`: Delimiter to use when concatenating the key fields (default: `-`).

## Usage
To use the `KeyToField` transformation, add it to your Kafka Connect connector configuration:
```
"transforms.Nomalizer.type": "com.vng.zshort.tools.connect.transform.normalizerfieldvalue.NormalizerFieldValueTransform",
"transforms.Nomalizer.field.name": "note",
"transforms.Nomalizer.field.result": "note_novn",
"transforms.Nomalizer.field.filters": "strip_html,noise_char,long_space",
```

### Example
Consider a Kafka topic with the following record:


```json
{
  "key": {
    "id": 123,
    "timestamp": 1644439200000
  },
  "value": {
    "order_id": 4,
    "note": "<b>hhhs</b> Đây là Tiếng Việt có dấu Đa vs         !@#%^&*(*(        yuuy dđ"
  }
}
```

After applying the `KeyToField` transformation, the record will be transformed as follows:

```json
{
  "key": {
    "id": 123,
    "timestamp": 1644439200000
  },
  "value": {
    "order_id": 4,
    "note": "<b>hhhs</b> Đây là Tiếng Việt có dấu Đa vs         !@#%^&*(*(        yuuy dđ",
    "note_novn": "hhhs day la tieng viet co dau da vs yuuy dd",
  }
}
```