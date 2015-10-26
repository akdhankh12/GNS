/*
 * Copyright (C) 2014
 * University of Massachusetts
 * All Rights Reserved 
 */
package edu.umass.cs.gns.utils;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Simple stats for short lists - not designed to be particularly fast or efficient.
 * 
 * @author westy
 */
public class Stats {

  ArrayList<Double> data;
  double size;

  /**
   *
   * @param data
   */
  public Stats(ArrayList<Double> data) {
    this.data = data;
    size = data.size();
  }
  
  /**
   *
   * @return
   */
  public int getN() {
    return (int) size;
  }

  /**
   *
   * @return
   */
  public double getMean() {
    double sum = 0.0;
    for (double a : data) {
      sum += a;
    }
    return sum / size;
  }

  /**
   *
   * @return
   */
  public double getVariance() {
    double mean = getMean();
    double temp = 0;
    for (double a : data) {
      temp += (mean - a) * (mean - a);
    }
    return temp / size;
  }

  /**
   *
   * @return
   */
  public double getStdDev() {
    return Math.sqrt(getVariance());
  }

  /**
   *
   * @return
   */
  public double median() {
    ArrayList<Double> copy = new ArrayList<Double>(data);
    Collections.sort(data);

    if (copy.size() % 2 == 0) {
      return (copy.get((copy.size() / 2) - 1) + copy.get(copy.size() / 2)) / 2.0;
    } else {
      return copy.get(copy.size() / 2);
    }
  }
}