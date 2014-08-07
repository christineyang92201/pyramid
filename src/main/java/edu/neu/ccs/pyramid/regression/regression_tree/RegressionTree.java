package edu.neu.ccs.pyramid.regression.regression_tree;

import edu.neu.ccs.pyramid.dataset.DataSet;
import edu.neu.ccs.pyramid.dataset.FeatureRow;
import edu.neu.ccs.pyramid.regression.Regressor;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import java.io.Serializable;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by chengli on 8/6/14.
 */
public class RegressionTree implements Regressor, Serializable {

    private static final long serialVersionUID = 1L;

    protected Node root;
    /**
     * the actual number of leaves may be smaller than maxNumLeaves
     */

    //todo seems can be transient
    protected List<Node> leaves;

    public RegressionTree() {
        this.leaves = new ArrayList<>();
    }

    /**
     *
     * @return number of leaves
     */
    protected int getNumLeaves(){
        return leaves.size();
    }

    public void fit(RegTreeConfig trainConfig) throws Exception{
        int maxNumLeaves = trainConfig.getMaxNumLeaves();

        this.root = new Node();
        //root gets all active data points
        this.root.setDataAppearance(trainConfig.getActiveDataPoints());
        //parallel
        updateNode(this.root, trainConfig);
        this.leaves.add(this.root);
        this.root.setLeaf(true);

        /**
         * grow the tree
         */
        while (this.getNumLeaves()<maxNumLeaves) {
            /**
             * first find the node which gives the max reduction once split
             */
            Optional<Node> leafToSplitOptional = this.findLeafToSplit();
            if (leafToSplitOptional.isPresent()){
                Node leafToSplit = leafToSplitOptional.get();
                this.splitNode(leafToSplit,trainConfig);
            } else {
                break;
            }
        }

        //parallel
        this.setLeavesOutputs(trainConfig);
        this.cleanLeaves();
    }


    public double predict(FeatureRow featureRow){
        return predict(featureRow.getVector());
    }

    private double predict(Vector vector){
        return predict(vector, this.root);
    }

    private double predict(Vector vector, Node node){
        if (node.isLeaf()){
            return node.getValue();
        } else if (vector.get(node.getFeatureIndex())<=node.getThreshold()){
            return predict(vector,node.getLeftChild());
        } else {
            return predict(vector,node.getRightChild());
        }
    }

//    public DecisionProcess getDecisionProcess(float [] featureRow,List<Feature> features){
//        StringBuilder sb = new StringBuilder();
//        Node nodeToCheck = this.root;
//        while (! this.leaves.contains(nodeToCheck)){
//            int featureIndex = nodeToCheck.getFeatureIndex();
//            float threshold = nodeToCheck.getThreshold();
//            if (featureRow[featureIndex] <= threshold){
//                nodeToCheck = nodeToCheck.getLeftChild();
//                sb.append(features.get(featureIndex).getFeatureName());
//                sb.append("(").append(featureRow[featureIndex]).append("<=").append(threshold).append(")  ");
//            }else{
//                nodeToCheck = nodeToCheck.getRightChild();
//                sb.append(features.get(featureIndex).getFeatureName());
//                sb.append("(").append(featureRow[featureIndex]).append(">").append(threshold).append(")  ");
//            }
//        }
//        return new DecisionProcess(sb.toString(),nodeToCheck.getValue());
//
//    }

    public List<Integer> getFeatureIndices(){
        List<Integer> featureIndices = new ArrayList<Integer>();
        LinkedBlockingDeque<Node> queue = new LinkedBlockingDeque<Node>();
        queue.offer(this.root);
        while(queue.size()!=0){
            Node node = queue.poll();
            if (! node.isLeaf()){
                //don't add the feature for leaf node, as it is useless
                featureIndices.add(node.getFeatureIndex());
                queue.offer(node.getLeftChild());
                queue.offer(node.getRightChild());
            }
        }
        return featureIndices;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Node node: this.leaves){
            Stack<Node> stack = new Stack<Node>();
            while(true){
                stack.push(node);
                if (node.getParent()==null){
                    break;
                }
                node = node.getParent();
            }
            while(!stack.empty()){
                Node node1 = stack.pop();
                if (!node1.isLeaf()){
                    Node node2 = stack.peek();
                    if (node2 == node1.getLeftChild()){
                        sb.append(node1.getFeatureIndex()+"<="+node1.getThreshold()+"   ");
                    } else {
                        sb.append(node1.getFeatureIndex()+">"+node1.getThreshold()+"   ");
                    }
                } else{
                    sb.append(": "+node1.getValue()+"\n");
                }
            }
        }
        return sb.toString();
    }

//    public String display(List<Feature> features){
//        StringBuilder sb = new StringBuilder();
//        for (Node node: this.leaves){
//            Stack<Node> stack = new Stack<Node>();
//            while(true){
//                stack.push(node);
//                if (node.getParent()==null){
//                    break;
//                }
//                node = node.getParent();
//            }
//            while(!stack.empty()){
//                Node node1 = stack.pop();
//                if (!node1.isLeaf()){
//                    Node node2 = stack.peek();
//                    if (node2 == node1.getLeftChild()){
//                        sb.append("feature "+node1.getFeatureIndex()+"("+features.get(node1.getFeatureIndex()).getFeatureName()+")"+"<="+node1.getThreshold()+"   ");
//                    } else {
//                        sb.append("feature "+node1.getFeatureIndex()+"("+features.get(node1.getFeatureIndex()).getFeatureName()+")"+">"+node1.getThreshold()+"   ");
//                    }
//                } else{
//                    sb.append(": "+node1.getValue()+"\n");
//                }
//            }
//        }
//        return sb.toString();
//    }

//    public Set<String> getSkipNgramNames(List<Feature> features){
//        Set<String> names = new HashSet<String>();
//        List<Integer> indices = this.getFeatureIndices();
//        for (int i:indices){
//            Feature ngram = features.get(i);
//            if (((Ngram)ngram).getNumTerms()>=2){
//                names.add(ngram.getFeatureName());
//            }
//        }
//        return names;
//    }

//    public String getRootFeatureName(List<Feature> features){
//        int featureIndex= this.root.getFeatureIndex();
//        return features.get(featureIndex).getFeatureName();
//    }

    public int getRootFeatureIndex(){
        return this.root.getFeatureIndex();
    }

    public double getRootRightOutput(){
        return this.root.getRightChild().getValue();
    }

    public double getRootReduction(){
        return this.root.getReduction();
    }



    protected Optional<Node> findLeafToSplit(){
        //TODO: using a heap data structure to find max reduction?
        return this.leaves.stream().filter(Node::isSplitable)
                .max(Comparator.comparing(Node::getReduction));
    }

    /**
     *
     * @param leafToSplit already splitable
     * @param trainConfig
     * @throws Exception
     */
    protected void splitNode(Node leafToSplit, RegTreeConfig trainConfig) throws Exception{
        DataSet dataSet = trainConfig.getDataSet();
        int maxNumLeaves = trainConfig.getMaxNumLeaves();
        /**
         * split this leaf node
         */
        int featureIndex = leafToSplit.getFeatureIndex();
        double threshold = leafToSplit.getThreshold();
        Vector inputVector = dataSet.getFeatureColumn(featureIndex).getVector();
        Vector columnVector;
        if (inputVector.isDense()){
            columnVector = inputVector;
        } else {
            columnVector = new DenseVector(inputVector);
        }
        /**
         * create children
         */
        Node leftChild = new Node();
        Node rightChild = new Node();
        int[] parentDataAppearance = leafToSplit.getDataAppearance();

        //update data appearance in children
        //<= go left, > go right


        // TODO: seems need to keep the order? can parallel? check
        int[] leftDataAppearance = Arrays.stream(parentDataAppearance).
                filter(i -> columnVector.get(i)<=threshold).toArray();
        int[] rightDataAppearance = Arrays.stream(parentDataAppearance).
                filter(i -> columnVector.get(i)>threshold).toArray();

        leftChild.setDataAppearance(leftDataAppearance);
        rightChild.setDataAppearance(rightDataAppearance);

        //the last two leaves need not to be updated completely
        //as we don't need to split them later
        if (this.getNumLeaves()!=maxNumLeaves-1){
            updateNode(leftChild,trainConfig);
            updateNode(rightChild,trainConfig);
        }


        /**
         * link left and right child to the parent
         */
        leafToSplit.setLeftChild(leftChild);
        leafToSplit.setRightChild(rightChild);
        leftChild.setParent(leafToSplit);
        rightChild.setParent(leafToSplit);

        /**
         * update leaves, remove the parent, and add children
         */
        this.leaves.remove(leafToSplit);
        leafToSplit.setLeaf(false);
        leafToSplit.clearDataAppearance();
        this.leaves.add(leftChild);
        leftChild.setLeaf(true);
        this.leaves.add(rightChild);
        rightChild.setLeaf(true);
    }

    protected void cleanLeaves(){
        for (Node leaf: this.leaves){
            leaf.clearDataAppearance();
        }
    }


    /**
     * parallel
     * @param trainConfig
     */
    private void setLeavesOutputs(RegTreeConfig trainConfig){
        this.leaves.parallelStream()
                .forEach(leaf -> this.setLeafOutput(leaf,trainConfig));
    }

    private void setLeafOutput(Node leaf, RegTreeConfig trainConfig){
        int[] dataAppearance = leaf.getDataAppearance();
        double output = trainConfig.getLeafOutputCalculator().getLeafOutput(dataAppearance);
        leaf.setValue(output);
    }

    /**
     * parallel
     * given dataAppearance, fill other information
     * @param node
     */
    protected void updateNode(Node node,RegTreeConfig trainConfig) throws Exception{
        Optional<SplitResult> splitResultOptional = Splitter.split(trainConfig,node.getDataAppearance());
        if (splitResultOptional.isPresent()){
            SplitResult splitResult = splitResultOptional.get();
            node.setFeatureIndex(splitResult.getFeatureIndex());
            node.setThreshold(splitResult.getThreshold());
            node.setReduction(splitResult.getReduction());
            node.setSplitable(true);
        } else{
            node.setSplitable(false);
        }
    }

}
