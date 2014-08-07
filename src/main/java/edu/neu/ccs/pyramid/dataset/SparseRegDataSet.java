package edu.neu.ccs.pyramid.dataset;

import java.io.File;
import java.util.Arrays;

/**
 * Created by chengli on 8/7/14.
 */
public class SparseRegDataSet extends SparseDataSet implements RegDataSet{
    private double[] labels;

    public SparseRegDataSet(int numDataPoints, int numFeatures) {
        super(numDataPoints, numFeatures);
        this.labels = new double[numDataPoints];
    }

    @Override
    public double[] getLabels() {
        return this.labels;
    }

    @Override
    public void setLabel(int dataPointIndex, double label) {
        this.labels[dataPointIndex]=label;
    }

    public static SparseRegDataSet loadStandard(File featureFile,
                                               File labelFile,
                                               String delimiter) throws Exception{
        int[] stats = DataSetUtil.parseStandard(featureFile,labelFile,delimiter);
        int numDataPoints = stats[0];
        int numFeatures = stats[1];
        System.out.println("loading data set from "+featureFile.getAbsolutePath()+
                " and "+labelFile.getAbsolutePath());
        System.out.println("number of data points = "+numDataPoints);
        System.out.println("number of features = "+numFeatures);
        SparseRegDataSet dataSet = new SparseRegDataSet(numDataPoints,numFeatures);
        DataSetUtil.loadStandard(dataSet,featureFile,labelFile,delimiter);
        System.out.println("data set loaded");
        return dataSet;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("number of data points = ").append(numDataPoints).append("\n");
        sb.append("number of features = ").append(numFeatures).append("\n");
        sb.append("=====================================").append("\n");
        sb.append("row matrix:").append("\n");
        for (int i=0;i<numDataPoints;i++){
            sb.append(i).append(":\t").append(getFeatureRow(i).getVector().asFormatString()).append("\n");
        }
        sb.append("=====================================").append("\n");
        sb.append("column matrix:").append("\n");
        for (int j=0;j<numFeatures;j++){
            sb.append(j).append(":\t").append(getFeatureColumn(j).getVector().asFormatString()).append("\n");
        }
        sb.append("labels = ").append(Arrays.toString(this.labels));
        return sb.toString();
    }

}
