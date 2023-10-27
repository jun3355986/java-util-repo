package com.jun.javautilrepo.other;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.enums.CellExtraTypeEnum;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.CellExtra;
import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     *  2. 然后用解决孤岛问题的方式把课表划分成各自独立的课表，根据班级、星期、第几节课的三个条件交汇成一个长方形区域，如坐标（firstColumnIndex":0,"firstRowIndex":0,"lastColumnIndex":9,"lastRowIndex":6）
     *  3. 接着找出班级、星期、第几节课的列或行，在结果二的区域内先找到（星期一、第一节课）这些信息，然后向右、向下查找，确定星期第几节是纵向或横向的并标记这些方向信息
     *  4. 读课程内容（课程名、老师），每找到一个课程、老师的内容，就在第三步中的列或行中找到班级、星期、第几节课的信息补充
     *
     *
     */

    /**
     * 课表类
     */
    public static class Timetable {
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

    enum TimetableCellType {

        CLASSNAME("班级"),
        WEEKDAY("星期"),
        PERIOD("第几节课"),
        TEACHER("老师"),
        COURSE_NAME("课程名");

        private String name;
        private TimetableCellType(String name) {
            this.name = name;
        }
    }

    public static class TimetableCell {
        private int x;
        private int y;
        private String content;
        private TimetableCellType type;

        public TimetableCell() {}

        public TimetableCell(int x, int y, String content) {
            this.x = x;
            this.y = y;
            this.content = content;
            if (isClassName(content)) {
                type = TimetableCellType.CLASSNAME;
            } else if (isWeekDay(content) ) {
                type = TimetableCellType.WEEKDAY;
            } else if (isPeriod(content)) {
                type = TimetableCellType.PERIOD;
            } else if (isTeacher(content)) {
                type = TimetableCellType.TEACHER;
            } else if (isCourseName(content)) {
                type = TimetableCellType.COURSE_NAME;
            }
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

        public static boolean isClassName(String str) {
            String regex = ".*([一二三四五六七八九]+)年级|(初|高)([一二三])|(大)([一二三四]).*";
            return regexMatch(regex, str);
        }

        public static boolean isWeekDay(String str) {
            String regex = ".*星期([一二三四五六日]+).*";
            return regexMatch(regex, str);
        }

        public static boolean isPeriod(String str) {
            String regex = ".*([1-9]|第([一二三四五六七八九]|[1-9])节).*";
            return regexMatch(regex, str);
        }

        public static boolean isTeacher(String str) {
            String regex = ".*([1-9]|第([一二三四五六七八九]|[1-9])节).*";
            return regexMatch(regex, str);
        }

        public static boolean isCourseName(String str) {
            String regex = ".*([1-9]|第([一二三四五六七八九]|[1-9])节).*";
            return regexMatch(regex, str);
        }

        public static boolean regexMatch(String regex, String str) {
            if (StringUtils.isBlank(str)) {
                return false;
            }
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(str);
            return matcher.matches();
        }
    }

    public static class ClassWeekPeriod {
        private List<TimetableCell> content;
        // 0:不分方向 1:横向 2:纵向
        private int direction;

        public ClassWeekPeriod() {
            this.content = new ArrayList<>();
            this.direction = 0;
        }

        public List<TimetableCell> getContent() {
            return content;
        }

        public void setContent(List<TimetableCell> content) {
            this.content = content;
        }

        public int getDirection() {
            return direction;
        }

        public void setDirection(int direction) {
            this.direction = direction;
        }
    }

    public static class TimetableListener extends AnalysisEventListener<Map<Integer, String>> {

        private int minCol = 0;
        private int minRow = 0;
        private int maxCol = 0;
        private int maxRow = 0;
        // 宽
        private int m;
        // 高
        private int n;
        private Map<Integer, Map<Integer, TimetableCell>> timetableCells = new HashMap<>(50);
        private Map<Integer, Map<Integer, TimetableCell>> timetableClassWeekPeriod = new HashMap<>(50);
        private Map<String, List<IslandDivide.Dot>> timeTableIsLandMap = new HashMap<>(50);
        private Map<String, ClassWeekPeriod> timetableClass = new HashMap<>(50);
        private Map<String, ClassWeekPeriod> timetableWeek = new HashMap<>(50);
        private Map<String, ClassWeekPeriod> timetablePeriod = new HashMap<>(50);

        // 这个方法会在每一行数据被读取后调用
        @Override
        public void invoke(Map<Integer, String> timetableCell, AnalysisContext context) {
            // 你可以在这里对读取到的数据进行处理，比如打印、存储等
            int curRow = context.readRowHolder().getRowIndex();
            log.info("读取第{}行的一条数据：{}", curRow, JSON.toJSONString(timetableCell));
            Map<Integer, TimetableCell> rowData = timetableCells.computeIfAbsent(curRow, k -> new HashMap<>(16));
            for (Map.Entry<Integer, String> entry : timetableCell.entrySet()) {
                String content = entry.getValue();
                int curCol = entry.getKey();

                if (curCol < minCol) {
                    minCol = curCol;
                }
                if (curCol > maxCol) {
                    maxCol = curCol;
                }
                if (curRow < minRow) {
                    minRow = curRow;
                }
                if (curRow > maxRow) {
                    maxRow = curRow;
                }
                TimetableCell timetableCell1 = new TimetableCell(curRow, curCol, content);
//                if (content == null) {
//                    log.info("内容为null");
//                }
//                log.info("课表单元信息： {}", JSONObject.toJSONString(timetableCell1));
                rowData.put(curCol, timetableCell1);
            }

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
                    log.info("获取第{}行信息，内容：{}", row, JSON.toJSONString(rowData ));
                    for (int col = extra.getFirstColumnIndex(); col <= extra.getLastColumnIndex(); col++) {
                        log.info("补全第{}行第{}列信息为：{}", row, col, firstTimetableCell.getContent());
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
            // 划分课表
            m = maxCol - minCol + 1;
            n = maxRow - minRow + 1;
            int[][] positions = new int[n*m][];
            int i = 0;
            for(Map.Entry<Integer, Map<Integer, TimetableCell>> rowData : timetableCells.entrySet()) {
                for (Map.Entry<Integer, TimetableCell> cell : rowData.getValue().entrySet()) {
                    if (StringUtils.isBlank(cell.getValue().getContent())) {
                        continue;
                    }
                    int[] cellXY = {cell.getValue().getX(), cell.getValue().getY()};
                    positions[i++] = cellXY;
                }
            }
            timeTableIsLandMap = IslandDivide.islandsList(m, n, positions);
            log.info("课程表划分数据：明细：{}", JSONObject.toJSONString(timeTableIsLandMap));

            // 提取课表中的班级、星期、节课信息
            for(Map.Entry<String, List< IslandDivide.Dot>> entry : timeTableIsLandMap.entrySet()) {
                String timetableNo = entry.getKey();
                for (IslandDivide.Dot dot : entry.getValue()) {
                    TimetableCell timetableCell = timetableCells.get(dot.getY()).get(dot.getX());
//                    if (Timetable.isClassName(timetableCell.getContent())) {
//                        if (CollectionUtils.isEmpty( timetableClass )) {
//                            timetableClass = new HashMap<>(16);
//                            ClassWeekPeriod classWeekPeriod = new ClassWeekPeriod();
//                        }
//                    } else if (Timetable.isWeekDay(timetableCell.getContent()) ) {
//
//                    } else if (Timetable.isPeriod(timetableCell.getContent())) {

//                    }
                }
            }

            // 补全核心内容（课程、老师）的其他三个信息


            System.out.println("所有数据读取完成");
        }
    }

    public static void main(String[] args) {
        TimetableListener timetableListener = new TimetableListener();
        EasyExcel.read("/Users/junjielong/Downloads/面试题-课表解析.xlsx", timetableListener)
                .headRowNumber(0)
                .extraRead(CellExtraTypeEnum.MERGE).sheet(4).doRead();



//        String str = "1";
//        String str1 = "2";
//        String str2 = "第五节";
//        String str3 = "第d";
//        Timetable timetable = new Timetable();
//        log.info("是否匹配：{}", timetable.isPeriod(str3) );
    }


}
