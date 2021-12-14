package com.julien.saevecke.learner.config.mealymachines.coffee;

import com.julien.saevecke.learner.oracles.membership.RabbitMQSulOracle;
import de.learnlib.algorithms.dhc.mealy.MealyDHC;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.oracle.equivalence.CompleteExplorationEQOracle;
import net.automatalib.automata.concepts.Output;
import net.automatalib.words.Word;
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
        var alphabet = Alphabets.fromArray(POD, CLEAN, WATER, BUTTON);
        var learner = new MealyDHC<>(alphabet, membershipOracle);
        var eq = new CompleteExplorationEQOracle<>(membershipOracle, 2, 3);

        DefaultQuery<String, Word<String>> counterexample = null;
        long start = System.nanoTime();
        do {
            if(counterexample == null) {
                learner.startLearning();
            } else {
                boolean refined = learner.refineHypothesis(counterexample);
                if (!refined) {
                    System.err.println("No refinement effected by counterexample!");
                }
            }

            counterexample = eq.findCounterExample(learner.getHypothesisModel(), alphabet);

        } while (counterexample != null);
        System.out.println("Learning complete");

        long finish = System.nanoTime();
        long timeElapsed = finish - start;

        // TODO: can be reduced by disable printing
        System.out.println("Learn time: " + timeElapsed/1000000 + " ms");
    }
}
