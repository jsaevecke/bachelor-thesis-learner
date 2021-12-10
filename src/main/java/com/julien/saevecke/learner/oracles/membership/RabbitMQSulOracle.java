package com.julien.saevecke.learner.oracles.membership;

import com.julien.saevecke.learner.config.rabbitmq.RabbitMQConfig;
import com.julien.saevecke.learner.messages.MembershipQuery;
import com.julien.saevecke.learner.proxy.DefaultQueryProxy;
import de.learnlib.api.oracle.MembershipOracle.MealyMembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.api.query.Query;
import net.automatalib.words.Word;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

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
                System.out.println("Waiting for completion...");
                completed = true;
                for (Query<String, Word<String>> rawQuery : queries) {
                    var defaultQuery = (DefaultQuery<String, Word<String>>)rawQuery;
                    if(defaultQuery.getOutput() == null || defaultQuery.getOutput().isEmpty()){
                        completed = false;
                        break;
                    }
                }
            }

            System.out.println("Completed!");

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

    @RabbitListener(queues = RabbitMQConfig.SUL_OUTPUT_QUEUE)
    public void consume(MembershipQuery query) {
        System.out.println("Message received from queue: " + query.toString());

        if(!sentQueries.containsKey(query.getUuid())) {
            return;
        }

        var defaultQuery = sentQueries.get(query.getUuid());
        defaultQuery.answer(Word.fromList(query.getQuery().getOutput()));
    }
}
