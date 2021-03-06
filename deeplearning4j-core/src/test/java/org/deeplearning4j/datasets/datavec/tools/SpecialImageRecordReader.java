package org.deeplearning4j.datasets.datavec.tools;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.Indexer;
import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.Writable;
import org.datavec.common.RecordConverter;
import org.datavec.common.data.NDArrayWritable;
import org.datavec.image.recordreader.ImageRecordReader;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author raver119@gmail.com
 */
@Slf4j
public class SpecialImageRecordReader extends ImageRecordReader {
    private AtomicInteger counter = new AtomicInteger(0);
    private AtomicInteger labelsCounter = new AtomicInteger(0);
    private int limit, channels, width, height, numClasses;
    private List<String> labels = new ArrayList<>();
    private INDArray zFeatures;



    public SpecialImageRecordReader(int totalExamples, int numClasses, int channels, int width, int height) {
        this.limit = totalExamples;
        this.channels = channels;
        this.width = width;
        this.height = height;
        this.numClasses = numClasses;

        for (int i = 0; i < numClasses; i++) {
            labels.add("" + i);
        }

        zFeatures =  Nd4j.create(128, channels, height, width);
    }

    @Override
    public boolean hasNext() {
        return counter.get() < limit;
    }


    @Override
    public void reset() {
        counter.set(0);
    }

    @Override
    public List<Writable> next() {
        INDArray features = Nd4j.create(channels, height, width);
        fillNDArray(features, counter.getAndIncrement());
        features = features.reshape(1, channels, height, width);
        List<Writable> ret = RecordConverter.toRecord(features);
        ret.add(new IntWritable(RandomUtils.nextInt(0, numClasses)));
        return ret;
    }

    public List<String> getLabels() {
        return labels;
    }


    @Override
    public boolean batchesSupported() {
        return true;
    }

    @Override
    public List<Writable> next(int num) {
        int numExamples = Math.min(num, limit - counter.get());
        //counter.addAndGet(numExamples);

        INDArray features = zFeatures;
        for (int i = 0; i < numExamples; i++) {
            fillNDArray(features.tensorAlongDimension(i, 1, 2, 3), counter.getAndIncrement());
        }

        INDArray labels = Nd4j.create(numExamples, numClasses);
        for (int i = 0; i < numExamples; i++){
            labels.getRow(i).assign(labelsCounter.getAndIncrement());
        }

        List<Writable> ret = RecordConverter.toRecord(features);
        ret.add(new NDArrayWritable(labels));

        return ret;
    }


    protected void fillNDArray(INDArray view, double value) {
        Pointer pointer = view.data().pointer();
        int[] shape = view.shape();
//        log.info("Shape: {}", Arrays.toString(shape));

        for (int c = 0; c < shape[0]; c++) {
            for (int h = 0; h < shape[1]; h++) {
                for (int w = 0; w < shape[2]; w++) {
                    view.putScalar(c, h, w, (float) value);
                }
            }
        }

/*
        if (pointer instanceof FloatPointer) {
            FloatIndexer idx = FloatIndexer.create((FloatPointer) pointer, new long[]{view.shape()[0], view.shape()[1], view.shape()[2]}, new long[]{view.stride()[0], view.stride()[1], view.stride()[2]});
            for (long c = 0; c < shape[0]; c++) {
                for (long h = 0; h < shape[1]; h++) {
                    for (long w = 0; w < shape[2]; w++) {
                        idx.put(c, h, w, (float) value);
                    }
                }
            }
        } else {
            DoubleIndexer idx = DoubleIndexer.create((DoublePointer) pointer, new long[]{view.shape()[0], view.shape()[1], view.shape()[2]}, new long[]{view.stride()[0], view.stride()[1], view.stride()[2]});
            for (long c = 0; c < shape[0]; c++) {
                for (long h = 0; h < shape[1]; h++) {
                    for (long w = 0; w < shape[2]; w++) {
                        idx.put(c, h, w, value);
                    }
                }
            }
        }
        */
    }
}
