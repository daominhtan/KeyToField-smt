package com.vng.zshort.tools.connect.transform.wrapfieldvalue;

import org.apache.kafka.common.cache.Cache;
import org.apache.kafka.common.cache.LRUCache;
import org.apache.kafka.common.cache.SynchronizedCache;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.transforms.util.SimpleConfig;
import org.apache.kafka.connect.transforms.util.SchemaUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.connect.transforms.util.Requirements.requireMap;
import static org.apache.kafka.connect.transforms.util.Requirements.requireStruct;

public class WrapFieldValueTransform<R extends ConnectRecord<R>> implements Transformation<R> {

    public static final String OVERVIEW_DOC = "Add the record key to the value as a named field.";

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define("field.name", ConfigDef.Type.STRING, "from_value", ConfigDef.Importance.HIGH,
                    "Name of the field to insert the Kafka key to")
            .define("field.result", ConfigDef.Type.STRING, "wrap_result", ConfigDef.Importance.HIGH,
                    "Name of the field to insert the Kafka key to")
            .define("wrapper.prefix", ConfigDef.Type.STRING, ",", ConfigDef.Importance.LOW,
                    "Prefix to use when concatenating the key fields")
            .define("wrapper.suffix", ConfigDef.Type.STRING, ",", ConfigDef.Importance.LOW,
                    "Suffix to use when concatenating the key fields");

    private static final String PURPOSE = "adding wrap value field to record";
    private static final Logger LOGGER = LoggerFactory.getLogger(WrapFieldValueTransform.class);
    private String fieldName;
    private String fieldResult;
    private String wrapperPrefix;
    private String wrapperSuffix;
    private Cache<Schema, Schema> schemaUpdateCache;

    @Override
    public void configure(Map<String, ?> props) {
        final SimpleConfig config = new SimpleConfig(CONFIG_DEF, props);
        fieldName = config.getString("field.name");
        fieldResult = config.getString("field.result");
        wrapperPrefix = config.getString("wrapper.prefix");
        wrapperSuffix = config.getString("wrapper.suffix");
        schemaUpdateCache = new SynchronizedCache<>(new LRUCache<>(16));
    }

    @Override
    public R apply(R record) {
        if (record.valueSchema() == null) {
            return applySchemaless(record);
        } else {
            return applyWithSchema(record);
        }
    }

    private R applySchemaless(R record) {
        LOGGER.trace("Applying SMT without a value schema");
        LOGGER.trace("Record key: {}", record.key());
        final Map<String, Object> value;
        if (record.value() == null) {
            value = new HashMap<>(1);
        } else {
            value = requireMap(record.value(), PURPOSE);
        }

        Object fieldValue = value.getOrDefault(fieldName, "");
        String wrapResult = wrapperPrefix + fieldValue + wrapperSuffix;

        value.put(fieldResult, wrapResult);

        LOGGER.trace("Wrap result string: {}", wrapResult);
        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                record.keySchema(),
                record.key(),
                record.valueSchema(),
                value,
                record.timestamp()
        );
    }

    private R applyWithSchema(R record) {
        LOGGER.trace("Applying SMT with a schema");
        LOGGER.trace("Record key: {}", record.key());

        final Struct value = requireStruct(record.value(), PURPOSE);

        Schema updatedSchema = schemaUpdateCache.get(value.schema());
        LOGGER.trace("Updated schema: {}", updatedSchema);
        if (updatedSchema == null) {
            updatedSchema = makeUpdatedSchema(value.schema());
            LOGGER.trace("Schema NULL updatedSchema: {}", updatedSchema.fields());
            schemaUpdateCache.put(value.schema(), updatedSchema);
        }

        final Struct updatedValue = new Struct(updatedSchema);
        LOGGER.trace("Updated value: {}", updatedValue);

        for (Field field : value.schema().fields()) {
            updatedValue.put(field.name(), value.get(field));
            if (field.name().equals(fieldName)) {
                Object fieldValue = value.get(fieldName);
                if (fieldValue == null) {
                    fieldValue = "";
                }
                String wrapResult = wrapperPrefix + fieldValue + wrapperSuffix;
                updatedValue.put(fieldResult, wrapResult);
                LOGGER.trace("Key as string: {}\nNew schema:{}", wrapResult, updatedValue.schema().fields());
            }
        }
        LOGGER.trace("Updated value after fields: {}", updatedValue);

        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                record.keySchema(),
                record.key(),
                updatedSchema,
                updatedValue,
                record.timestamp()
        );
    }

    private Schema makeUpdatedSchema(Schema schema) {
        LOGGER.trace("build the updated schema");
        SchemaBuilder newSchemabuilder = SchemaUtil.copySchemaBasics(schema, SchemaBuilder.struct());
        for (org.apache.kafka.connect.data.Field field : schema.fields()) {
            newSchemabuilder.field(field.name(), field.schema());
        }

        LOGGER.trace("adding the new field: {}", fieldResult);
        newSchemabuilder.field(fieldResult, Schema.OPTIONAL_STRING_SCHEMA);
        return newSchemabuilder.build();
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public void close() {
        schemaUpdateCache = null;
    }
}
