https://juejin.cn/post/6975515503078932516
https://mybatis.org/mybatis-3/zh_CN/dynamic-sql.html


https://blog.csdn.net/u014644574/article/details/105695787

https://www.cnblogs.com/ysySelf/p/11549383.html

在这段sql的基础上增加startime1，endtime1,startime2，endtime2的时间段，要求统计这两段时间内占比


你可以通过在原有 SQL 的基础上增加时间条件来统计 `a` 列值在指定的时间段内的占比。假设你的表中有一个时间列 `timestamp_column`，你可以使用如下 SQL 语句：

```sql
SELECT
    ROUND(SUM(CASE WHEN a <= -20 AND timestamp_column BETWEEN starttime1 AND endtime1 THEN 1 ELSE 0 END) / 
          NULLIF(SUM(CASE WHEN timestamp_column BETWEEN starttime1 AND endtime1 THEN 1 ELSE 0 END), 0) * 100, 6) AS ratio_negative_20_time1,
    
    ROUND(SUM(CASE WHEN a > -20 AND a <= 0 AND timestamp_column BETWEEN starttime1 AND endtime1 THEN 1 ELSE 0 END) / 
          NULLIF(SUM(CASE WHEN timestamp_column BETWEEN starttime1 AND endtime1 THEN 1 ELSE 0 END), 0) * 100, 6) AS ratio_negative_to_zero_time1,
    
    ROUND(SUM(CASE WHEN a > 0 AND timestamp_column BETWEEN starttime1 AND endtime1 THEN 1 ELSE 0 END) / 
          NULLIF(SUM(CASE WHEN timestamp_column BETWEEN starttime1 AND endtime1 THEN 1 ELSE 0 END), 0) * 100, 6) AS ratio_positive_time1,

    ROUND(SUM(CASE WHEN a <= -20 AND timestamp_column BETWEEN starttime2 AND endtime2 THEN 1 ELSE 0 END) / 
          NULLIF(SUM(CASE WHEN timestamp_column BETWEEN starttime2 AND endtime2 THEN 1 ELSE 0 END), 0) * 100, 6) AS ratio_negative_20_time2,
    
    ROUND(SUM(CASE WHEN a > -20 AND a <= 0 AND timestamp_column BETWEEN starttime2 AND endtime2 THEN 1 ELSE 0 END) / 
          NULLIF(SUM(CASE WHEN timestamp_column BETWEEN starttime2 AND endtime2 THEN 1 ELSE 0 END), 0) * 100, 6) AS ratio_negative_to_zero_time2,
    
    ROUND(SUM(CASE WHEN a > 0 AND timestamp_column BETWEEN starttime2 AND endtime2 THEN 1 ELSE 0 END) / 
          NULLIF(SUM(CASE WHEN timestamp_column BETWEEN starttime2 AND endtime2 THEN 1 ELSE 0 END), 0) * 100, 6) AS ratio_positive_time2

FROM your_table;
```

### 解释：
1. **时间条件**：在 `SUM(CASE WHEN ...)` 中添加了 `timestamp_column BETWEEN starttime1 AND endtime1` 和 `timestamp_column BETWEEN starttime2 AND endtime2` 条件，确保只统计在这两个时间段内的记录。
2. **`NULLIF`**：避免除以零的情况，如果在指定时间段内没有记录，返回 `null`，确保计算不出错。
3. **六位小数**：使用 `ROUND(..., 6)` 保留六位小数。

这个查询将分别计算在两个时间段内的占比，并返回六个列，分别对应于 `(-∞, -20]`、`(-20, 0]` 和 `(0, +∞)` 的占比。



将上面的sql使用mybatis语法简化一下


你可以使用 MyBatis 的 `<foreach>` 标签来替代重复的 SQL 语句部分，从而简化代码。以下是一个示例，演示如何使用循环来统计不同时间段的占比：

```xml
<mapper namespace="your.namespace.Mapper">

    <select id="getRatio" resultType="java.util.Map">
        SELECT
            <foreach item="interval" index="index" collection="timeIntervals" separator=",">
                ROUND(SUM(CASE WHEN a <= -20 AND timestamp_column BETWEEN #{interval.start} AND #{interval.end} THEN 1 ELSE 0 END) / 
                      NULLIF(SUM(CASE WHEN timestamp_column BETWEEN #{interval.start} AND #{interval.end} THEN 1 ELSE 0 END), 0) * 100, 6) AS ratio_negative_20_time${index + 1},
                
                ROUND(SUM(CASE WHEN a > -20 AND a <= 0 AND timestamp_column BETWEEN #{interval.start} AND #{interval.end} THEN 1 ELSE 0 END) / 
                      NULLIF(SUM(CASE WHEN timestamp_column BETWEEN #{interval.start} AND #{interval.end} THEN 1 ELSE 0 END), 0) * 100, 6) AS ratio_negative_to_zero_time${index + 1},
                
                ROUND(SUM(CASE WHEN a > 0 AND timestamp_column BETWEEN #{interval.start} AND #{interval.end} THEN 1 ELSE 0 END) / 
                      NULLIF(SUM(CASE WHEN timestamp_column BETWEEN #{interval.start} AND #{interval.end} THEN 1 ELSE 0 END), 0) * 100, 6) AS ratio_positive_time${index + 1}
            </foreach>
        FROM your_table
    </select>

</mapper>
```

### 说明：
1. **`<foreach>`**：用于循环遍历 `timeIntervals` 集合，该集合包含多个时间段的开始和结束时间。
    - `item`：代表当前遍历的时间段对象。
    - `index`：当前遍历的索引，可以用来生成不同的列名。
    - `collection`：传入的时间段集合。
    - `separator`：每个生成的 SQL 语句之间的分隔符。

2. **占比计算**：在循环中动态生成每个时间段的占比计算，避免了重复的 SQL 代码。

### Java 代码：
在调用此 SQL 的 Java 方法中，你需要准备一个 `List<Map<String, Object>>` 或其他数据结构来传递时间段，例如：

```java
List<Map<String, Object>> timeIntervals = new ArrayList<>();
Map<String, Object> interval1 = new HashMap<>();
interval1.put("start", starttime1);
interval1.put("end", endtime1);
timeIntervals.add(interval1);

Map<String, Object> interval2 = new HashMap<>();
interval2.put("start", starttime2);
interval2.put("end", endtime2);
timeIntervals.add(interval2);

// 调用 MyBatis 方法
Map<String, Object> result = yourMapper.getRatio(timeIntervals);
```

这样，你就可以在 SQL 中灵活地处理多个时间段，并保持代码的简洁性和可维护性。




计算表中a,b两列分别在两段时间内的平均值之差，以及这两个差之间的差值

你可以使用 SQL 来计算表中 `a` 和 `b` 两列在两个时间段内的平均值之差，以及这两个差之间的差值。以下是一个示例 SQL 查询：

```sql
SELECT
    AVG(a1) - AVG(a2) AS avg_diff_a,
    AVG(b1) - AVG(b2) AS avg_diff_b,
    (AVG(a1) - AVG(a2)) - (AVG(b1) - AVG(b2)) AS diff_between_diffs
FROM (
    SELECT
        AVG(a) AS a1,
        AVG(b) AS b1
    FROM your_table
    WHERE timestamp_column BETWEEN #{starttime1} AND #{endtime1}
) AS time1,
(
    SELECT
        AVG(a) AS a2,
        AVG(b) AS b2
    FROM your_table
    WHERE timestamp_column BETWEEN #{starttime2} AND #{endtime2}
) AS time2;
```

### 说明：
1. **`AVG(a)` 和 `AVG(b)`**：分别计算 `a` 列和 `b` 列在指定时间段内的平均值。
2. **子查询**：使用两个子查询分别计算第一个时间段 (`starttime1` 和 `endtime1`) 和第二个时间段 (`starttime2` 和 `endtime2`) 的平均值。
3. **计算差值**：
   - `avg_diff_a`：计算 `a` 列在两个时间段的平均值之差。
   - `avg_diff_b`：计算 `b` 列在两个时间段的平均值之差。
   - `diff_between_diffs`：计算这两个差之间的差值。

### Java 代码示例：
在 Java 代码中调用这个 SQL 方法时，你需要传入时间参数：

```java
Map<String, Object> result = yourMapper.calculateAvgDiff(starttime1, endtime1, starttime2, endtime2);
```

这样，你就可以得到表中 `a` 和 `b` 列在两个时间段内的平均值之差，以及这两个差之间的差值。







在 Java 中，计算除法并取小数点后六位可以使用 `BigDecimal` 类来精确控制小数位数。代码示例如下：

```java
import java.math.BigDecimal;
import java.math.RoundingMode;

public class DecimalExample {
    public static void main(String[] args) {
        // 定义两个数
        BigDecimal num1 = new BigDecimal("10");
        BigDecimal num2 = new BigDecimal("3");

        // 执行除法运算并取小数点后六位
        BigDecimal result = num1.divide(num2, 6, RoundingMode.HALF_UP);

        // 输出结果
        System.out.println("结果为: " + result);
    }
}
```

### 代码说明：
1. **`divide` 方法**：`num1.divide(num2, 6, RoundingMode.HALF_UP)` 执行除法运算，并保留小数点后 6 位。
2. **`RoundingMode.HALF_UP`**：采用四舍五入的方式进行小数点处理。

这样可以保证输出的结果精确到小数点后 6 位。