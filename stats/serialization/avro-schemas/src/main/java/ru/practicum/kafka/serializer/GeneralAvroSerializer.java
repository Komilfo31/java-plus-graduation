package ru.practicum.kafka.serializer;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

public class GeneralAvroSerializer implements Serializer<SpecificRecordBase> {
    private final EncoderFactory encoderFactory;
    private BinaryEncoder encoder;

    public GeneralAvroSerializer() {
        this(EncoderFactory.get());
    }

    public GeneralAvroSerializer(EncoderFactory encoderFactory) {
        this.encoderFactory = Objects.requireNonNull(encoderFactory, "encoderFactory must not be null");
    }

    @Override
    public byte[] serialize(String topic, SpecificRecordBase data) {
        Objects.requireNonNull(topic, "topic must not be null");

        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            encoder = encoderFactory.binaryEncoder(out, encoder);

            Schema schema = data.getSchema();
            DatumWriter<SpecificRecordBase> writer = new SpecificDatumWriter<>(schema);
            writer.write(data, encoder);

            encoder.flush();
            return out.toByteArray();

        } catch (IOException ex) {
            throw new SerializationException(
                    String.format("Failed to serialize data for topic [%s]", topic),
                    ex
            );
        }
    }

    @Override
    public void close() {
        if (encoder != null) {
            try {
            } catch (Exception e) {
            }
        }
    }
}
