package com.julien.saevecke.learner.config.mealymachines.coffee;

import com.julien.saevecke.learner.oracles.membership.RabbitMQSulOracle;
import de.learnlib.algorithms.dhc.mealy.MealyDHC;
import net.automatalib.words.impl.Alphabets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class Experiment {
    public static final String POD = "POD";
    public static final String CLEAN = "CLEAN";
    public static final String WATER = "WATER";
    public static final String BUTTON = "BUTTON";

    @Autowired
    RabbitMQSulOracle membershipOracle;

    @PostConstruct
    public void learn() {
        var learner = new MealyDHC<>(Alphabets.fromArray(POD, CLEAN, WATER, BUTTON), membershipOracle);

        learner.startLearning();

        System.out.println(learner.getHypothesisModel());
    }
}
