package com.jun.javautilrepo.other;

import java.util.ArrayList;
import java.util.List;

/**
 * @className: DensityCluster
 * @description: TODO 类描述
 * @author: jdt
 * @date: 2023/10/26 05:34
 **/
// 定义一个密度聚类类，包含数据集、参数和方法
public class DensityCluster {

    // 定义一个数据点类，包含坐标、类型和所属簇
    public static class DataPoint {
        private double x; // x坐标
        private double y; // y坐标
        private int type; // 类型，-1表示噪声，0表示未访问，1表示核心或边界
        private int cluster; // 所属簇，-1表示未分配

        public DataPoint(double x, double y) {
            this.x = x;
            this.y = y;
            this.type = 0;
            this.cluster = -1;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getCluster() {
            return cluster;
        }

        public void setCluster(int cluster) {
            this.cluster = cluster;
        }
    }

    private List<DataPoint> dataSet; // 数据集
    private double eps; // 邻域半径
    private int minPts; // 邻域内最小点数

    public DensityCluster(List<DataPoint> dataSet, double eps, int minPts) {
        this.dataSet = dataSet;
        this.eps = eps;
        this.minPts = minPts;
    }

    // 计算两个数据点之间的欧氏距离
    public double getDistance(DataPoint p1, DataPoint p2) {
        return Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2));
    }

    // 获取某个数据点的邻域内的所有数据点
    public List<DataPoint> getNeighbours(DataPoint p) {
        List<DataPoint> neighbours = new ArrayList<>();
        for (DataPoint q : dataSet) {
            if (getDistance(p, q) <= eps) {
                neighbours.add(q);
            }
        }
        return neighbours;
    }

    // 对数据集进行密度聚类
    public void cluster() {
        int clusterId = 0; // 簇的编号，从0开始递增
        for (DataPoint p : dataSet) {
            if (p.getType() != 0) { // 如果该点已经访问过，跳过
                continue;
            }
            List<DataPoint> neighbours = getNeighbours(p); // 获取该点的邻域内的所有数据点
            if (neighbours.size() < minPts) { // 如果邻域内的数据点数小于minPts，标记该点为噪声
                p.setType(-1);
            } else { // 否则，创建一个新的簇，并将该点和其邻域内的数据点加入到该簇中
                p.setType(1);
                p.setCluster(clusterId);
                expandCluster(p, neighbours, clusterId); // 扩展该簇
                clusterId++; // 簇的编号递增
            }
        }
    }

    // 扩展某个簇，对邻域内的每个数据点进行判断和处理
    public void expandCluster(DataPoint p, List<DataPoint> neighbours, int clusterId) {
        for (int i = 0; i < neighbours.size(); i++) {
            DataPoint q = neighbours.get(i); // 获取邻域内的第i个数据点
            if (q.getType() == 0) { // 如果该点未访问过，标记为边界点，并加入到当前簇中
                q.setType(1);
                q.setCluster(clusterId);
                List<DataPoint> subNeighbours = getNeighbours(q); // 获取该点的邻域内的所有数据点
                if (subNeighbours.size() >= minPts) { // 如果该点的邻域内的数据点数大于等于minPts，将其邻域内的数据点加入到原来的邻域中
                    neighbours.addAll(subNeighbours);
                }
            }
            if (q.getCluster() == -1) { // 如果该点未分配到任何簇，将其加入到当前簇中
                q.setCluster(clusterId);
            }
        }
    }

    // 打印聚类结果
    public void printResult() {
        System.out.println("数据集共有" + dataSet.size() + "个数据点");
        System.out.println("eps = " + eps + ", minPts = " + minPts);
        int noiseCount = 0; // 噪声点的数量
        int clusterCount = 0; // 簇的数量
        for (DataPoint p : dataSet) {
            if (p.getType() == -1) { // 统计噪声点的数量
                noiseCount++;
            }
            if (p.getCluster() > clusterCount) { // 统计簇的数量
                clusterCount = p.getCluster();
            }
        }
        clusterCount++; // 簇的编号从0开始，所以实际数量要加1
        System.out.println("噪声点的数量为：" + noiseCount);
        System.out.println("簇的数量为：" + clusterCount);
        for (int i = 0; i < clusterCount; i++) { // 打印每个簇的信息
            System.out.println("第" + (i + 1) + "个簇：");
            for (DataPoint p : dataSet) {
                if (p.getCluster() == i) { // 如果该点属于当前簇，打印其坐标和类型
                    System.out.println("(" + p.getX() + "," + p.getY() + ") " + (p.getType() == 1 ? "核心或边界" : "噪声"));
                }
            }
        }
    }

}
