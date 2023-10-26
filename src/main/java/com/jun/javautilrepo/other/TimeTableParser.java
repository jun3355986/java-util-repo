package com.jun.javautilrepo.other;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.enums.CellExtraTypeEnum;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.CellExtra;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @className: TimeTableParser
 * @description: TODO 类描述
 * @author: jdt
 * @date: 2023/10/25 17:59
 **/
@Slf4j
public class TimeTableParser {

    // 解析excel文件，提取数据格式为[{"七年级(8)班","星期一",1,"李晓侠","英语"}, {"七年级(8)班","星期二",1,"李晓侠","英语"}]

    /**
     * 10x6矩阵
     * 不同类型的课表现为矩阵的1. 转置 2. reshape运算
     * 实现方案一：
     * 1. 先全部数据扫描
     * 2. 每读到一个数据，给数据标记信息：数据本身，xy坐标，
     * 3. 剔除无用数据列或行 （处理x或y坐标跨度多个属性列其中有无效数据列【除了班级以外】）
     * 4. 进行矩阵运行成标准矩阵（标准课表）（判断哪些矩阵需要做哪些运算）
     * 5. 解析标准课表
     *
     * 实现方法二：
     * 1. 可以从核心内容（老师，课程）出发，补全另外三个信息班级、星期、第几节课
     * 2. 识别出一个课表整体
     *      定义一个完整课表的范围：
     *      0. 班级、星期、第几节课决定了一个完整课程的二维边界
     *      1. 有班级、星期、第几节、老师、课程名信息
     *      2. 星期、第几节课都在同一列
     *      3. 首先从左到右，从上到下，先定星期或第几节课（为什么不先定位班级，因为定位了班级后，不好定位该课表的星期或节课）
     *      4. 在年级的位置
     * 3. 在边界内识别星期、第几节课、班级在第几行或第几列
     * 4. 核心内容（老师，课程）就在步骤3中找到对应的星期、第几节和班级信息补全
     *
     * 实现方法二：
     *  1. 先把一个sheet的全部数据读出来，并给每一个数据记录数据内容本身、x坐标、y坐标
     *  2. 然后用处理并查集的方式把课表划分成各自独立的课表，根据班级、星期、第几节课的三个条件交汇成一个长方形区域，如坐标（firstColumnIndex":0,"firstRowIndex":0,"lastColumnIndex":9,"lastRowIndex":6）
     *  3. 接着找出班级、星期、第几节课的列或行，在结果二的区域内先找到（星期一、第一节课）这些信息，然后向右、向下查找，确定星期第几节是纵向或横向的并标记这些方向信息
     *  4. 读课程内容（课程名、老师），每找到一个课程、老师的内容，就在第三步中的列或行中找到班级、星期、第几节课的信息补充
     *
     *
     */

    /**
     * 课表类
     */
    public class Timetable {
        private String className;
        private String weekDay;
        private String period;
        private String teacher;
        private String courseName;

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getWeekDay() {
            return weekDay;
        }

        public void setWeekDay(String weekDay) {
            this.weekDay = weekDay;
        }

        public String getPeriod() {
            return period;
        }

        public void setPeriod(String period) {
            this.period = period;
        }

        public String getTeacher() {
            return teacher;
        }

        public void setTeacher(String teacher) {
            this.teacher = teacher;
        }

        public String getCourseName() {
            return courseName;
        }

        public void setCourseName(String courseName) {
            this.courseName = courseName;
        }
    }

    public static class TimetableCell {
        private int x;
        private int y;
        private String content;

        public TimetableCell() {}

        public TimetableCell(int x, int y, String content) {
            this.x = x;
            this.y = y;
            this.content = content;
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

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class TimetableListener extends AnalysisEventListener<Map<Integer, String>> {

        private List<Map<Integer, TimetableCell>> timetableCells = new ArrayList<>(100);

        // 这个方法会在每一行数据被读取后调用
        @Override
        public void invoke(Map<Integer, String> timetableCell, AnalysisContext context) {
            // 你可以在这里对读取到的数据进行处理，比如打印、存储等
            //System.out.println("读取到一条数据：" + timetableCell);
            Map<Integer, TimetableCell> rowData = new HashMap<>(16);
            for (Map.Entry<Integer, String> entry : timetableCell.entrySet()) {
                String content = entry.getValue();
                TimetableCell timetableCell1 = new TimetableCell(context.readRowHolder().getRowIndex(), entry.getKey(), content);
//                if (content == null) {
//                    log.info("内容为null");
//                }
//                log.info("课表单元信息： {}", JSONObject.toJSONString(timetableCell1));
                rowData.put(entry.getKey(), timetableCell1);
            }
            timetableCells.add(rowData);
        }

        @Override
        public void extra(CellExtra extra, AnalysisContext context) {
            //log.info("读取到了一条额外信息,text:{} other:{}", extra.getText(), JSON.toJSONString(extra));
            if (extra.getType().equals(CellExtraTypeEnum.MERGE )) {
                log.info(
                        "额外信息是合并单元格,而且覆盖了一个区间,在firstRowIndex:{},firstColumnIndex;{},lastRowIndex:{},lastColumnIndex:{}",
                        extra.getFirstRowIndex(), extra.getFirstColumnIndex(), extra.getLastRowIndex(),
                        extra.getLastColumnIndex());
                // 补全合并的单元格信息（因为只有第一个cell会读到，其他为null）
                TimetableCell firstTimetableCell = timetableCells.get(extra.getFirstRowIndex()).get(extra.getFirstColumnIndex());
                for (int row = extra.getFirstRowIndex(); row <= extra.getLastRowIndex(); row++ ) {
                    Map<Integer, TimetableCell> rowData = timetableCells.get(row);
                    for (int col = extra.getFirstColumnIndex(); col <= extra.getLastColumnIndex(); col++) {
                        TimetableCell timetableCell1 = new TimetableCell(row, col, firstTimetableCell.getContent());
                        rowData.put(col, timetableCell1);
                    }
                }
            }
        }

        // 这个方法会在所有数据被读取后调用
        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 你可以在这里做一些收尾工作，比如关闭资源等
            log.info("课表内容：{}", JSONObject.toJSONString(timetableCells));
            System.out.println("所有数据读取完成");
        }
    }

    public static void main(String[] args) {
        EasyExcel.read("/Users/junjielong/Downloads/面试题-课表解析.xlsx", new TimetableListener())
                .headRowNumber(0)
                .extraRead(CellExtraTypeEnum.MERGE).sheet(1).doRead();
    }


}
