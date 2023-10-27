package com.jun.javautilrepo.other;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @className: IslandDivide
 * @description: TODO 类描述
 * @author: jdt
 * @date: 2023/10/27 03:08
 **/
@Slf4j
public class IslandDivide {

    public static class Dot {
        private int x;
        private int y;

        public Dot(int x, int y)  {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }

    public static List<Integer> numIslands(int m, int n, int[][] positions) {
        UnionFind uf = new UnionFind();
        List<Integer> ans = new ArrayList<>();
        for (int[] position : positions) {
            ans.add(uf.connect(position[0], position[1]));
        }
        return ans;
    }

    public static Map<String, List<Dot>> islandsList(int m, int n, int[][] positions) {
        UnionFind uf = new UnionFind();
        List<Integer> ans = new ArrayList<>();
        for (int[] position : positions) {
            if(position == null) {
                break;
            }
            ans.add(uf.connect(position[0], position[1]));
        }

        log.info("课程表划分数据：集合数：{}", uf.sets);

        Map<String, List<Dot>> dotsMap = new HashMap<>(16);
        for (Map.Entry<String, String> entry : uf.getParent().entrySet()){
            List<Dot> dots = dotsMap.computeIfAbsent(entry.getValue(), k -> new ArrayList<>());
            String[] xy = entry.getKey().split("_");
            dots.add(new Dot(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
        }
        return dotsMap;
    }

    public static class UnionFind {
        private HashMap<String, String> parent;
        private HashMap<String, Integer> size;
        private ArrayList<String> help;
        private int sets;

        public UnionFind() {
            parent = new HashMap<>();
            size = new HashMap<>();
            help = new ArrayList<>();
            sets = 0;
        }

        private String findParent(String cur) {
            while (!cur.equals(parent.get(cur))) {
                help.add(cur);
                cur = parent.get(cur);
            }
            for (String str : help) {
                parent.put(str, cur);
            }
            help.clear();
            return cur;
        }

        private void union(String s1, String s2) {
            if (parent.containsKey(s1) && parent.containsKey(s2)) {
                String f1 = findParent(s1);
                String f2 = findParent(s2);
                if (!f1.equals(f2)) {
                    int size1 = size.get(f1);
                    int size2 = size.get(f2);
                    String big = size1 >= size2 ? f1 : f2;
                    String small = big == f1 ? f2 : f1;
                    parent.put(small, big);
                    size.put(big, size1 + size2);
                    sets--;
                }
            }
        }

        public int connect(int r, int c) {
            String key = String.valueOf(r) + "_" + String.valueOf(c);
            if (!parent.containsKey(key)) {
                parent.put(key, key);
                size.put(key, 1);
                sets++;
                String up = String.valueOf(r - 1) + "_" + String.valueOf(c);
                String down = String.valueOf(r + 1) + "_" + String.valueOf(c);
                String left = String.valueOf(r) + "_" + String.valueOf(c - 1);
                String right = String.valueOf(r) + "_" + String.valueOf(c + 1);
                union(up, key);
                union(down, key);
                union(left, key);
                union(right, key);
            }
            return sets;
        }

        public HashMap<String, String> getParent() {
            return parent;
        }

        public void setParent(HashMap<String, String> parent) {
            this.parent = parent;
        }

        public HashMap<String, Integer> getSize() {
            return size;
        }

        public void setSize(HashMap<String, Integer> size) {
            this.size = size;
        }

        public int getSets() {
            return sets;
        }

        public void setSets(int sets) {
            this.sets = sets;
        }
    }

}
