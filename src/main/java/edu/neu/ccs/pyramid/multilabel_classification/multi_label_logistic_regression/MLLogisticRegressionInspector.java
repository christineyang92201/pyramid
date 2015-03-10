package edu.neu.ccs.pyramid.multilabel_classification.multi_label_logistic_regression;

import edu.neu.ccs.pyramid.classification.logistic_regression.LogisticRegression;
import edu.neu.ccs.pyramid.feature.FeatureList;
import edu.neu.ccs.pyramid.feature.FeatureUtility;
import org.apache.mahout.math.Vector;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by chengli on 2/5/15.
 */
public class MLLogisticRegressionInspector {

    public static List<FeatureUtility> topFeatures(MLLogisticRegression logisticRegression,
                                                   int k){
        FeatureList featureList = logisticRegression.getFeatureList();
        Vector weights = logisticRegression.getWeights().getWeightsWithoutBiasForClass(k);
        Comparator<FeatureUtility> comparator = Comparator.comparing(FeatureUtility::getUtility);
        List<FeatureUtility> list = IntStream.range(0, weights.size())
                .mapToObj(i -> new FeatureUtility(featureList.get(i)).setUtility(weights.get(i)))
                .filter(featureUtility -> featureUtility.getUtility()>0)
                .sorted(comparator.reversed())
                .collect(Collectors.toList());
        IntStream.range(0,list.size()).forEach(i-> list.get(i).setRank(i));
        return list;
    }
}
