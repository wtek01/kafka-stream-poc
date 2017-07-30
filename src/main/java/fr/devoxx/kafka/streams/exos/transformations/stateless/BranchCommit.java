package fr.devoxx.kafka.streams.exos.transformations.stateless;

import fr.devoxx.kafka.conf.AppConfiguration;
import fr.devoxx.kafka.streams.pojo.GitMessage;
import fr.devoxx.kafka.streams.pojo.serde.PojoJsonSerializer;
import fr.devoxx.kafka.utils.AppUtils;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;

import java.util.HashMap;
import java.util.Map;


public class BranchCommit {

    private static final String NAME = "BranchCommit";
    private static final String APP_ID = AppUtils.appID(NAME);

    public static void main(String[] args) {

        KStreamBuilder kStreamBuilder = new KStreamBuilder();
        StreamsConfig config = new StreamsConfig(AppConfiguration.getProperties(APP_ID));

        final Serde<String> stringSerde = Serdes.String();
        Map<String, Object> serdeProps = new HashMap<>();

        final PojoJsonSerializer<GitMessage> jsonSerializer = new PojoJsonSerializer<>(GitMessage.class.getName());
        serdeProps.put(GitMessage.class.getName(), GitMessage.class);
        jsonSerializer.configure(serdeProps, false);

        final Serde<GitMessage> messageSerde = Serdes.serdeFrom(jsonSerializer, jsonSerializer);


        //START EXO

        run(kStreamBuilder, stringSerde, messageSerde);

        //STOP EXO


        System.out.println("Starting Kafka Streams " + NAME + " Example");
        KafkaStreams kafkaStreams = new KafkaStreams(kStreamBuilder, config);
        kafkaStreams.cleanUp();
        kafkaStreams.start();
        System.out.println("Now started  " + NAME + "  Example");

    }

    public static void run(KStreamBuilder kStreamBuilder, Serde<String> stringSerde, Serde<GitMessage> messageSerde) {
        KStream<String, GitMessage> messagesStream =
                kStreamBuilder.stream(stringSerde, messageSerde, AppConfiguration.SCALA_GITLOG_TOPIC)
                        .map((k, v) -> KeyValue.pair(v.getHash(), v));

        KStream<String, GitMessage>[] commit = messagesStream.branch(
                (key, value) -> value.getMessage() != null && value.getMessage().toLowerCase().contains("fix"),
                (key, value) -> value.getMessage() != null && !value.getMessage().toLowerCase().contains("fix")
        );


        commit[0].to(stringSerde, messageSerde, NAME + "_FIX");
        commit[1].to(stringSerde, messageSerde, NAME + "_FEAT");
    }


}