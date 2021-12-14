package com.julien.saevecke.learner.oracles.membership;

import com.julien.saevecke.learner.config.rabbitmq.RabbitMQConfig;
import com.julien.saevecke.learner.sul.messages.MembershipQuery;
import com.julien.saevecke.learner.proxy.DefaultQueryProxy;
import de.learnlib.api.oracle.MembershipOracle.MealyMembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.api.query.Query;
import net.automatalib.words.Word;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class RabbitMQSulOracle implements MealyMembershipOracle<String, String> {
    @Autowired
    AmqpTemplate template;

    HashMap<UUID, DefaultQuery<String, Word<String>>> sentQueries = new HashMap<>();

    @Override
    public void processQueries(Collection<? extends Query<String, Word<String>>> queries) {
        for (Query<String, Word<String>> rawQuery : queries) {
            var uuid = UUID.randomUUID();
            var defaultQuery = (DefaultQuery<String, Word<String>>)rawQuery;
            var query = new MembershipQuery(uuid, DefaultQueryProxy.createFrom(defaultQuery));
            sentQueries.put(uuid, defaultQuery);

            System.out.println("Sent query: " + query.getQuery().getPrefix() + " | " + query.getQuery().getSuffix());

            template.convertAndSend(
                    RabbitMQConfig.SUL_DIRECT_EXCHANGE,
                    RabbitMQConfig.SUL_INPUT_ROUTING_KEY,
                    query
            );
        }

        System.out.println("Messages sent!");

        var latch = new CountDownLatch(1);

        // wait until all queries are answered
        Thread newThread = new Thread(()->{
            var completed = false;

            while(!completed) {
                completed = true;
                var message = template.receiveAndConvert(RabbitMQConfig.SUL_OUTPUT_QUEUE);
                if (message == null) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                        completed = false;
                        continue;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                var query = (MembershipQuery)message;

                if(sentQueries.containsKey(query.getUuid())) {
                    var defaultQuery = sentQueries.get(query.getUuid());
                    defaultQuery.answer(Word.fromList(query.getQuery().getOutput()));

                    System.out.println("Received: " + query.getQuery().getPrefix() + " | " + query.getQuery().getSuffix() + " --> " + query.getQuery().getOutput());
                } else {
                    System.out.println("Unknown message received - drop!");
                    completed = false;
                    continue;
                }

                //TODO: can be done more efficient
                for (Query<String, Word<String>> rawQuery : queries) {
                    var defaultQuery = (DefaultQuery<String, Word<String>>)rawQuery;
                    if(defaultQuery.getOutput() == null || defaultQuery.getOutput().isEmpty()){
                        completed = false;
                        break;
                    }
                }
            }

            System.out.println("All messages answered!");

            sentQueries.clear();

            latch.countDown();
        });

        newThread.start();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
    public void consume(MembershipQuery query) {
        System.out.println("Message received from queue: " + query.toString());

        if(!sentQueries.containsKey(query.getUuid())) {
            return;
        }

        var defaultQuery = sentQueries.get(query.getUuid());
        defaultQuery.answer(Word.fromList(query.getQuery().getOutput()));
    }*/
}
